package com.anxin.browser.core.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import com.anxin.browser.core.download.DownloadRepository
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.MediaType
import com.anxin.browser.core.model.TabSession
import com.anxin.browser.core.util.Urls
import dev.anxin.engine.aw.AnxinEngine
import dev.anxin.engine.aw.AnxinEngine.AnxinTab
import dev.anxin.engine.aw.AnxinEngine.AnxinContentsClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.chromium.base.Callback
import org.json.JSONArray
import org.json.JSONTokener
import java.io.File
import java.util.ArrayDeque
import java.util.UUID

/**
 * M3 阶段二内核：AwContents 直嵌，Chromium 内核随 APK 分发，不依赖系统 WebView Provider。
 *
 * 启用方式：编译 Gradle 时加 `-PuseChromiumKernel=true`，并把 GN 产出的
 * `anxin_engine_aw.aar` 放到 `anxin-browser/libs/`，同时拷入 .so / .pak 资源。
 * 关闭该属性时本文件不参与编译，工程仍走 [WebViewEngine]。
 */
class ChromiumEngine(
    private val context: Context,
    private val downloads: DownloadRepository,
) : BrowserEngine, ChromiumEngineHooks {

    private var visitListener: ((title: String, url: String) -> Unit)? = null
    private var scriptProvider: ((url: String) -> List<String>)? = null

    override fun setVisitListener(listener: (title: String, url: String) -> Unit) {
        visitListener = listener
    }

    override fun setPageScriptProvider(provider: (url: String) -> List<String>) {
        scriptProvider = provider
    }

    init {
        AnxinEngine.init(
            context.applicationContext,
            context.applicationContext.getSharedPreferences("anxin_webview_flags", Context.MODE_PRIVATE),
        )
    }

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    override val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    override val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _mediaResources = MutableStateFlow<List<MediaResource>>(emptyList())
    override val mediaResources: StateFlow<List<MediaResource>> = _mediaResources.asStateFlow()

    private val tabsById = LinkedHashMap<String, AnxinTab>()
    private val unloadedUrls = LinkedHashMap<String, String>()
    private val findResults = HashMap<String, Int>()
    private val closedUrls = ArrayDeque<BrowserEngine.ClosedEntry>()
    override val closedTabCount: Int get() = closedUrls.size
    override val closedTabs: List<BrowserEngine.ClosedEntry>
        get() = closedUrls.toList().asReversed()

    private var textZoomPercent: Int = 100
    private var desktopMode: Boolean = false
    private var forceDark: Boolean = false
    private var adBlockEnabled: Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun createTab(url: String, incognito: Boolean): BrowserTab {
        val id = UUID.randomUUID().toString()
        val native = AnxinEngine.createTab(context.applicationContext, true)
        tabsById[id] = native

        val client = native.client()
        client.adBlockEnabled = adBlockEnabled
        client.pageEventListener = object : AnxinContentsClient.PageEventListener {
            override fun onPageStarted(startedUrl: String) {
                update(id) {
                    it.copy(
                        url = startedUrl,
                        isLoading = true,
                        progress = 0,
                        faviconUrl = Urls.favicon(startedUrl),
                    )
                }
            }

            override fun onProgressChanged(progress: Int) {
                update(id) { it.copy(progress = progress, isLoading = progress < 100) }
            }

            override fun onTitleChanged(title: String) {
                if (title.isNotBlank()) update(id) { it.copy(title = title) }
            }

            override fun onPageFinished(finishedUrl: String, title: String) {
                val currentTitle = title.ifBlank { client.title }
                update(id) {
                    it.copy(
                        url = finishedUrl,
                        title = currentTitle.ifBlank { it.title },
                        isLoading = false,
                        progress = 100,
                        faviconUrl = Urls.favicon(finishedUrl),
                        canGoBack = native.canGoBack(),
                        canGoForward = native.canGoForward(),
                    )
                }
                native.evaluateJavaScript(SNIFFER_SCRIPT, null)
                if (!incognito) runCatching { visitListener?.invoke(currentTitle, finishedUrl) }
                scriptProvider?.let { provider ->
                    runCatching {
                        provider(finishedUrl).forEach { script ->
                            native.evaluateJavaScript(script, null)
                        }
                    }
                }
            }

            override fun onFindResult(activeOrdinal: Int, matches: Int) {
                findResults[id] = matches
            }
        }
        client.downloadHandler = AnxinContentsClient.DownloadHandler {
                url, userAgent, contentDisposition, mimeType, contentLength ->
            val referer = _tabs.value.firstOrNull { it.id == id }?.url.orEmpty()
            downloads.enqueue(
                url = url,
                mimeType = mimeType,
                contentDisposition = contentDisposition,
                contentLength = contentLength,
                userAgent = userAgent,
                referer = referer,
            )
        }
        client.externalSchemeHandler = AnxinContentsClient.ExternalSchemeHandler { schemeUrl ->
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(schemeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            }.getOrDefault(true)
        }
        native.addJavascriptInterface(MediaBridge(id), "AnxinHost")
        native.setTextZoom(textZoomPercent)
        native.setUserAgent(userAgent(desktopMode))
        native.setForceDark(forceDark)

        val tab = BrowserTab(
            id = id,
            url = url,
            isIncognito = incognito,
            webView = native.view(),
            lastActiveAt = System.currentTimeMillis(),
        )
        _tabs.value = _tabs.value + tab
        _activeTabId.value = id
        if (url.isNotBlank() && url != "about:blank") native.loadUrl(url)
        return tab
    }

    override fun closeTab(tabId: String) {
        val closing = _tabs.value.firstOrNull { it.id == tabId }
        tabsById.remove(tabId)?.destroy()
        findResults.remove(tabId)
        unloadedUrls.remove(tabId)
        if (closing != null && !closing.isIncognito && isHttpUrl(closing.url)) {
            closedUrls.addLast(BrowserEngine.ClosedEntry(closing.url, closing.title, System.currentTimeMillis()))
            while (closedUrls.size > 20) closedUrls.removeFirst()
        }
        val remaining = _tabs.value.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) _activeTabId.value = remaining.lastOrNull()?.id
        _mediaResources.value = _mediaResources.value.filterNot { it.sourceTabId == tabId }
    }

    override fun restoreLastClosed(): BrowserTab? {
        val entry = closedUrls.removeLastOrNull() ?: return null
        return createTab(entry.url)
    }

    override fun restoreClosed(entry: BrowserEngine.ClosedEntry): BrowserTab? {
        closedUrls.remove(entry) ?: return null
        return createTab(entry.url)
    }

    override fun clearClosedTabs() {
        closedUrls.clear()
    }

    override fun restoreSession(sessions: List<TabSession>, activeIndex: Int): Boolean {
        if (sessions.isEmpty() || _tabs.value.isNotEmpty()) return false
        val restored = mutableListOf<BrowserTab>()
        sessions.forEach { session ->
            if (isHttpUrl(session.url)) {
                val tab = createTab("about:blank", session.incognito)
                // listener 已在 createTab 挂好；此处只记录待加载地址，选中时才 loadUrl。
                restored += tab.copy(url = session.url)
            }
        }
        if (restored.isEmpty()) return false
        val active = restored.getOrNull(activeIndex.coerceIn(0, restored.lastIndex)) ?: restored.first()
        restored.forEach { if (it.id != active.id) unloadedUrls[it.id] = it.url }
        _tabs.value = restored
        _activeTabId.value = active.id
        unloadedUrls.remove(active.id)
        tabsById[active.id]?.loadUrl(active.url)
        return true
    }

    override fun closeAllIncognito() {
        _tabs.value.filter { it.isIncognito }.forEach { closeTab(it.id) }
        tabsById.values.forEach { it.clearCache(true) }
    }

    override fun selectTab(tabId: String) {
        if (!tabsById.containsKey(tabId)) return
        _activeTabId.value = tabId
        _tabs.value = _tabs.value.map {
            if (it.id == tabId) it.copy(lastActiveAt = System.currentTimeMillis()) else it
        }
        unloadedUrls.remove(tabId)?.let { tabsById[tabId]?.loadUrl(it) }
    }

    override fun activeContentView(): Any? = _activeTabId.value?.let { tabsById[it]?.view() }

    override fun loadUrl(url: String) {
        val target = Urls.normalize(url)
        val id = _activeTabId.value ?: createTab(target).id
        tabsById[id]?.loadUrl(target)
    }

    override fun goBack(): Boolean {
        val tab = tabsById[_activeTabId.value ?: return false] ?: return false
        if (!tab.canGoBack()) return false
        tab.goBack()
        return true
    }

    override fun goForward(): Boolean {
        val tab = tabsById[_activeTabId.value ?: return false] ?: return false
        if (!tab.canGoForward()) return false
        tab.goForward()
        return true
    }

    override fun reload() {
        tabsById[_activeTabId.value ?: return]?.reload()
    }

    override fun stopLoading() {
        tabsById[_activeTabId.value ?: return]?.stopLoading()
    }

    override fun setTextZoom(percent: Int) {
        textZoomPercent = percent.coerceIn(50, 200)
        tabsById.values.forEach { it.setTextZoom(textZoomPercent) }
    }

    override fun setDesktopMode(enabled: Boolean) {
        desktopMode = enabled
        tabsById.values.forEach { it.setUserAgent(userAgent(enabled)) }
        reload()
    }

    override fun setForceDark(enabled: Boolean) {
        forceDark = enabled
        tabsById.values.forEach { it.setForceDark(enabled) }
    }

    override fun findInPage(text: String): Int {
        if (text.isBlank()) return 0
        val id = _activeTabId.value ?: return 0
        val tab = tabsById[id] ?: return 0
        findResults[id] = 0
        tab.findAllAsync(text)
        // AwContents 的查找结果异步回调；这里返回上一次结果，实时命中数经回调更新。
        return findResults[id] ?: 0
    }

    override fun setAdBlock(enabled: Boolean) {
        adBlockEnabled = enabled
        tabsById.values.forEach { it.client().adBlockEnabled = enabled }
    }

    override fun extractReadablePage(callback: (String?) -> Unit) {
        val tab = tabsById[_activeTabId.value] ?: run { callback(null); return }
        tab.evaluateJavaScript(READER_SCRIPT, object : Callback<String> {
            override fun onResult(result: String?) {
                if (result == null) {
                    callback(null)
                    return
                }
                val decoded = runCatching {
                    when (val value = JSONTokener(result.trim()).nextValue()) {
                        is String -> value
                        org.json.JSONObject.NULL -> null
                        else -> value?.toString()
                    }
                }.getOrNull()
                callback(decoded)
            }
        })
    }

    override fun createPrintAdapter(title: String): Any? = null

    override fun saveOfflinePage(): String? {
        val tab = tabsById[_activeTabId.value] ?: return null
        val title = _tabs.value.firstOrNull { it.id == _activeTabId.value }?.title ?: "page"
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "offline")
        if (!dir.exists()) dir.mkdirs()
        val safeName = title.replace(Regex("[\\\\/:*?\"<>|\n]"), "_").take(40)
            .ifBlank { "page_${System.currentTimeMillis()}" }
        val file = File(dir, "$safeName.mht")
        runCatching { tab.saveWebArchive(file.absolutePath, null) }
        return file.absolutePath
    }

    override fun clearMediaResources() {
        _mediaResources.value = emptyList()
    }

    override fun clearBrowsingData() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        tabsById.values.forEach { it.clearCache(true) }
    }

    override fun destroy() {
        tabsById.values.forEach { it.destroy() }
        tabsById.clear()
        _tabs.value = emptyList()
        _activeTabId.value = null
    }

    private fun update(tabId: String, block: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) block(it) else it }
    }

    private fun isHttpUrl(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")

    private fun ArrayDeque<String>.removeLastOrNull(): String? =
        if (isEmpty()) null else removeLast()

    private fun userAgent(desktop: Boolean): String = if (desktop) {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    } else {
        "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 AnxinBrowser/0.3"
    }

    /** 页面层媒体嗅探桥：JS 通过 AnxinHost.onMedia 上报。 */
    private inner class MediaBridge(private val tabId: String) {
        @JavascriptInterface
        fun onMedia(payload: String) {
            runCatching {
                val array = JSONArray(payload)
                val found = mutableListOf<MediaResource>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val url = obj.optString("url").takeIf { it.isNotBlank() } ?: continue
                    found += MediaResource(
                        url = url,
                        type = typeOf(url, obj.optString("type")),
                        title = obj.optString("title").takeIf { it.isNotBlank() },
                        sourceTabId = tabId,
                    )
                }
                if (found.isNotEmpty()) {
                    _mediaResources.value = (_mediaResources.value + found)
                        .distinctBy { it.url }
                        .takeLast(60)
                }
            }
        }
    }

    private fun typeOf(url: String, hint: String = ""): MediaType = when {
        url.contains(".m3u8", true) -> MediaType.HLS
        url.contains(".mpd", true) -> MediaType.DASH
        hint.equals("video", true) -> MediaType.VIDEO
        hint.equals("audio", true) -> MediaType.AUDIO
        Regex("\\.(mp4|webm|flv|mkv|mov|ts)(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(url) -> MediaType.VIDEO
        Regex("\\.(mp3|m4a|aac|flac|wav|ogg)(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(url) -> MediaType.AUDIO
        Regex("\\.(jpg|jpeg|png|webp|gif)(\\?|$)", RegexOption.IGNORE_CASE).containsMatchIn(url) -> MediaType.IMAGE
        else -> MediaType.FILE
    }

    companion object {
        /** 供 AppGraph 通过反射创建，避免关闭可选内核时产生编译期强引用。 */
        @JvmStatic
        fun create(context: Context, downloads: DownloadRepository): BrowserEngine =
            ChromiumEngine(context, downloads)

        const val SNIFFER_SCRIPT = """
        (function(){
          if (window.__anxinSnifferInstalled) return;
          window.__anxinSnifferInstalled = true;
          function report(list){
            try { if (list.length) AnxinHost.onMedia(JSON.stringify(list)); } catch(e) {}
          }
          function scan(){
            var out = [];
            try {
              document.querySelectorAll('video, audio, source').forEach(function(el){
                var src = el.currentSrc || el.src || el.getAttribute('src');
                if (src && src.indexOf('data:') !== 0) {
                  out.push({url: src, type: el.tagName.toLowerCase(), title: document.title});
                }
              });
              document.querySelectorAll('a[href]').forEach(function(a){
                var h = a.getAttribute('href') || '';
                if (/\.(mp4|m3u8|mpd|mp3|m4a|flv|webm|ts)(\?|$)/i.test(h)) {
                  out.push({url: a.href, type: 'file', title: (a.textContent || '').trim().slice(0, 60)});
                }
              });
            } catch(e) {}
            report(out);
          }
          setTimeout(scan, 900);
          setTimeout(scan, 3000);
          document.addEventListener('click', function(){ setTimeout(scan, 1500); }, true);
        })();
        """

        const val READER_SCRIPT = """
        (function(){
          try {
            var paras = [];
            var nodes = document.querySelectorAll('p, h1, h2, h3, h4, li, div[class*="para"], article');
            for (var i = 0; i < nodes.length; i++) {
              var t = (nodes[i].innerText || '').trim();
              if (t.length >= 24) paras.push(t);
            }
            var text = paras.join('\n\n');
            if (text.length < 400) {
              text = (document.body.innerText || '').replace(/\n{3,}/g, '\n\n').slice(0, 20000);
            }
            return JSON.stringify({title: document.title || '', text: text.slice(0, 20000)});
          } catch (e) { return null; }
        })();
        """
    }
}
