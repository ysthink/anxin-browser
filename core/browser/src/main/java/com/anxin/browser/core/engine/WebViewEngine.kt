package com.anxin.browser.core.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintDocumentAdapter
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.anxin.browser.core.download.DownloadRepository
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.MediaType
import com.anxin.browser.core.model.TabSession
import com.anxin.browser.core.util.Urls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.util.ArrayDeque
import java.util.UUID

/**
 * 第一阶段内核实现：Android WebView。
 *
 * 定位说明：
 * - 主页 / 标签 / 下载 / 嗅探等上层业务只依赖 [BrowserEngine]；
 * - 正式接入 Titanium（Chromium）后，本类可整体替换为 ChromiumEngine，
 *   UI 与业务层不需要改动；
 * - WebView 的隐私隔离能力弱于 Chromium 的 OTR Profile，此处仅做
 *   「不落缓存 + 不记历史」级别的近似实现，真实隔离需内核层支持。
 */
class WebViewEngine(
    private val context: Context,
    private val downloads: DownloadRepository,
    /** 访问记录回调：仅普通标签的 http(s) 页面触发，无痕标签不回调。 */
    private val onVisit: (title: String, url: String) -> Unit = { _, _ -> },
) : BrowserEngine {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    override val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    override val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _mediaResources = MutableStateFlow<List<MediaResource>>(emptyList())
    override val mediaResources: StateFlow<List<MediaResource>> = _mediaResources.asStateFlow()

    private val views = LinkedHashMap<String, WebView>()

    /** 关闭标签恢复栈（仅记录可恢复的 http(s) 地址）。 */
    private val closedUrls = ArrayDeque<String>()
    override val closedTabCount: Int get() = closedUrls.size

    // 网页浏览设置（应用到新标签 + 存量标签）
    private var textZoomPercent: Int = 100
    private var desktopMode: Boolean = false
    private var forceDark: Boolean = false

    override fun createTab(url: String, incognito: Boolean): BrowserTab {
        val id = UUID.randomUUID().toString()
        val webView = buildWebView(id, incognito)
        views[id] = webView
        val tab = BrowserTab(
            id = id,
            url = url,
            isIncognito = incognito,
            webView = webView,
            lastActiveAt = System.currentTimeMillis(),
        )
        _tabs.value = _tabs.value + tab
        _activeTabId.value = id
        if (url.isNotBlank() && url != "about:blank") webView.loadUrl(url)
        return tab
    }

    override fun closeTab(tabId: String) {
        val closing = _tabs.value.firstOrNull { it.id == tabId }
        val view = views.remove(tabId)
        if (view != null) {
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        if (closing != null && !closing.isIncognito &&
            (closing.url.startsWith("http://") || closing.url.startsWith("https://"))
        ) {
            closedUrls.addLast(closing.url)
            while (closedUrls.size > 20) closedUrls.removeFirst()
        }
        val remaining = _tabs.value.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.lastOrNull()?.id
        }
        _mediaResources.value = _mediaResources.value.filterNot { it.sourceTabId == tabId }
    }

    override fun restoreLastClosed(): BrowserTab? {
        val url = closedUrls.removeLastOrNull_()
        if (url == null) return null
        return createTab(url)
    }

    // 会话恢复时记录"待懒加载"的标签：选中时才真正 loadUrl。
    private val unloadedUrls = LinkedHashMap<String, String>()

    override fun restoreSession(sessions: List<TabSession>, activeIndex: Int): Boolean {
        if (sessions.isEmpty()) return false
        if (_tabs.value.isNotEmpty()) return false
        val restored = mutableListOf<BrowserTab>()
        sessions.forEach { session ->
            if (session.url.startsWith("http://") || session.url.startsWith("https://")) {
                val id = UUID.randomUUID().toString()
                val webView = buildWebView(id, session.incognito)
                views[id] = webView
                restored += BrowserTab(
                    id = id,
                    url = session.url,
                    isIncognito = session.incognito,
                    webView = webView,
                    lastActiveAt = System.currentTimeMillis(),
                )
            }
        }
        if (restored.isEmpty()) return false
        val active = restored.getOrNull(activeIndex.coerceIn(0, restored.lastIndex)) ?: restored.first()
        restored.forEach { tab ->
            if (tab.id != active.id) unloadedUrls[tab.id] = tab.url
        }
        _tabs.value = restored
        _activeTabId.value = active.id
        views[active.id]?.loadUrl(active.url)
        return true
    }

    override fun closeAllIncognito() {
        _tabs.value.filter { it.isIncognito }.forEach { closeTab(it.id) }
        views.values.forEach { it.clearCache(true) }
    }

    override fun selectTab(tabId: String) {
        if (views.containsKey(tabId)) {
            _activeTabId.value = tabId
            _tabs.value = _tabs.value.map {
                if (it.id == tabId) it.copy(lastActiveAt = System.currentTimeMillis()) else it
            }
            // 会话恢复的标签：首次选中时懒加载
            unloadedUrls.remove(tabId)?.let { url ->
                views[tabId]?.loadUrl(url)
            }
        }
    }

    override fun activeContentView(): Any? = _activeTabId.value?.let { views[it] }

    override fun loadUrl(url: String) {
        val target = Urls.normalize(url)
        val id = _activeTabId.value ?: createTab(target).id
        views[id]?.loadUrl(target)
    }

    override fun goBack(): Boolean {
        val view = views[_activeTabId.value ?: return false] ?: return false
        if (!view.canGoBack()) return false
        view.goBack()
        return true
    }

    override fun goForward(): Boolean {
        val view = views[_activeTabId.value ?: return false] ?: return false
        if (!view.canGoForward()) return false
        view.goForward()
        return true
    }

    override fun reload() {
        val id = _activeTabId.value ?: return
        views[id]?.reload()
    }

    override fun stopLoading() {
        val id = _activeTabId.value ?: return
        views[id]?.stopLoading()
    }

    override fun setTextZoom(percent: Int) {
        textZoomPercent = percent.coerceIn(50, 200)
        views.values.forEach { it.settings.textZoom = textZoomPercent }
    }

    override fun setDesktopMode(enabled: Boolean) {
        desktopMode = enabled
        views.values.forEach { it.settings.userAgentString = userAgent(enabled) }
        // UA 变更需刷新页面才能生效
        reload()
    }

    override fun setForceDark(enabled: Boolean) {
        forceDark = enabled
        views.values.forEach { it.isForceDarkAllowed = enabled }
    }

    @Suppress("DEPRECATION")
    override fun findInPage(text: String): Int {
        if (text.isBlank()) return 0
        val view = views[_activeTabId.value] ?: return 0
        return runCatching { view.findAll(text) }.getOrDefault(0)
    }

    override fun createPrintAdapter(title: String): Any? {
        val view = views[_activeTabId.value] ?: return null
        return runCatching { view.createPrintDocumentAdapter(title) as PrintDocumentAdapter }.getOrNull()
    }

    override fun saveOfflinePage(): String? {
        val view = views[_activeTabId.value] ?: return null
        val title = _tabs.value.firstOrNull { it.id == _activeTabId.value }?.title ?: "page"
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "offline")
        if (!dir.exists()) dir.mkdirs()
        val name = title.replace(Regex("[\\\\/:*?\"<>|\n]"), "_").take(40)
            .ifBlank { "page_${System.currentTimeMillis()}" }
        val file = File(dir, "$name.mht")
        return runCatching {
            view.saveWebArchive(file.absolutePath, false, null)
            file.takeIf { it.exists() }?.absolutePath
        }.getOrNull()
    }

    override fun clearMediaResources() {
        _mediaResources.value = emptyList()
    }

    override fun clearBrowsingData() {
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        views.values.forEach { it.clearCache(true) }
    }

    override fun destroy() {
        views.values.forEach {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        views.clear()
    }

    private fun userAgent(desktop: Boolean): String {
        if (desktop) {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
        return "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 AnxinBrowser/0.2"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(tabId: String, incognito: Boolean): WebView {
        val webView = WebView(context)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            supportZoom()
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            userAgentString = userAgent(desktopMode)
            textZoom = textZoomPercent
        }
        webView.isForceDarkAllowed = forceDark
        webView.addJavascriptInterface(MediaBridge(tabId), "AnxinHost")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val current = url ?: return
                update(tabId) {
                    it.copy(url = current, isLoading = true, progress = 0, faviconUrl = Urls.favicon(current))
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                update(tabId) {
                    it.copy(
                        url = url ?: it.url,
                        title = view.title?.takeIf { t -> t.isNotBlank() } ?: it.title,
                        isLoading = false,
                        progress = 100,
                        canGoBack = view.canGoBack(),
                        canGoForward = view.canGoForward(),
                    )
                }
                view.evaluateJavascript(SNIFFER_SCRIPT, null)
                if (!incognito && url != null) {
                    runCatching { onVisit(view.title ?: "", url) }
                }
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                update(tabId) {
                    it.copy(
                        url = url ?: it.url,
                        title = view.title?.takeIf { t -> t.isNotBlank() } ?: it.title,
                        canGoBack = view.canGoBack(),
                        canGoForward = view.canGoForward(),
                    )
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("http://", true) || url.startsWith("https://", true)) return false
                return runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                }.getOrDefault(true)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                update(tabId) { it.copy(progress = newProgress, isLoading = newProgress < 100) }
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!title.isNullOrBlank()) update(tabId) { it.copy(title = title) }
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloads.enqueue(
                url = url,
                mimeType = mimeType,
                contentDisposition = contentDisposition,
                contentLength = contentLength,
                userAgent = userAgent,
                referer = webView.url,
            )
        }
        return webView
    }

    private fun update(tabId: String, block: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) block(it) else it }
    }

    private fun ArrayDeque<String>.removeLastOrNull_(): String? =
        if (isEmpty()) null else removeLast()

    /** 页面层嗅探桥：接收注入脚本上报的媒体资源。 */
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
                    val merged = (_mediaResources.value + found)
                        .distinctBy { it.url }
                        .takeLast(60)
                    _mediaResources.value = merged
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

    private companion object {
        /**
         * 页面层嗅探脚本：监听 video/audio/source 与常见的媒体直链。
         * 后续接入内核层 Network Observer 后，可与此脚本互补覆盖更多站点。
         */
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
    }
}
