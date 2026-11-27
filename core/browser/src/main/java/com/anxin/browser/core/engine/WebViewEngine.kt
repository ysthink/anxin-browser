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

    /** 关闭窗口恢复栈（仅记录可恢复的 http(s) 地址）。 */
    private val closedUrls = ArrayDeque<BrowserEngine.ClosedEntry>()
    override val closedTabCount: Int get() = closedUrls.size
    override val closedTabs: List<BrowserEngine.ClosedEntry>
        get() = closedUrls.toList().asReversed()

    // 网页浏览设置（应用到新标签 + 存量标签）
    private var textZoomPercent: Int = 100
    private var desktopMode: Boolean = false
    private var forceDark: Boolean = false
    private var adBlockEnabled: Boolean = false

    /**
     * 扩展脚本提供者：返回该 URL 需要注入的用户脚本（类扩展）。
     * 由 App 层（ScriptExtensionBridge）提供，内核不感知扩展实现。
     */
    var pageScriptProvider: ((url: String) -> List<String>)? = null

    override fun setAdBlock(enabled: Boolean) {
        adBlockEnabled = enabled
    }

    override fun extractReadablePage(callback: (String?) -> Unit) {
        val view = views[_activeTabId.value] ?: run { callback(null); return }
        view.evaluateJavascript(READER_SCRIPT) { result ->
            // evaluateJavascript 返回的是 JSON 字面量（带外层引号/转义），先解一层
            val decoded = runCatching {
                val value = org.json.JSONTokener(result?.trim() ?: "null").nextValue()
                when (value) {
                    is String -> value
                    org.json.JSONObject.NULL -> null
                    else -> value?.toString()
                }
            }.getOrNull()
            callback(decoded)
        }
    }

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
            closedUrls.addLast(BrowserEngine.ClosedEntry(closing.url, closing.title, System.currentTimeMillis()))
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
        val entry = closedUrls.removeLastOrNull_()
        if (entry == null) return null
        return createTab(entry.url)
    }

    override fun restoreClosed(entry: BrowserEngine.ClosedEntry): BrowserTab? {
        closedUrls.remove(entry) ?: return null
        return createTab(entry.url)
    }

    override fun clearClosedTabs() {
        closedUrls.clear()
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
        views.values.forEach { applyDarkMode(it) }
        // 注意：不在这里 reload。算法深色是否生效取决于宿主 Configuration 的
        // uiMode（夜间位），而 App 的夜间开关不改 uiMode——此时刷新只会丢掉
        // 滚动位置却依然白屏。要让网页真正变暗，需要让 Activity 的 uiMode 跟随
        // 这个开关（会触发 recreate），属于独立一档改造，见 docs 的待办项。
    }

    /**
     * 夜间模式要让网页一起暗下来。
     *
     * 原来的 `isForceDarkAllowed` 只在"App 主题为浅色 + 系统为深色"的自动判定下才生效，
     * 而我们是自己实现的夜间开关（不改 App 主题），所以网页始终白底——夜里点开网页就被糊一脸光。
     * API 33 起用显式的算法深色，29~32 回落到 forceDark 开关。
     */
    private fun applyDarkMode(view: WebView) {
        view.isForceDarkAllowed = forceDark
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            runCatching { view.settings.setAlgorithmicDarkeningAllowed(forceDark) }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            runCatching {
                view.settings.forceDark =
                    if (forceDark) android.webkit.WebSettings.FORCE_DARK_ON
                    else android.webkit.WebSettings.FORCE_DARK_OFF
            }
        }
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
        applyDarkMode(webView)
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
                // 类扩展注入（用户脚本，按站点匹配）
                if (url != null) {
                    pageScriptProvider?.let { provider ->
                        runCatching {
                            provider(url).forEach { script ->
                                view.evaluateJavascript(script, null)
                            }
                        }
                    }
                }
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

            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest,
            ): android.webkit.WebResourceResponse? {
                if (!adBlockEnabled) return super.shouldInterceptRequest(view, request)
                val host = try { Uri.parse(request.url.toString()).host.orEmpty() } catch (_: Exception) { "" }
                if (host.isNotBlank() && AD_HOST_KEYWORDS.any { host.contains(it, ignoreCase = true) }) {
                    return android.webkit.WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
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
            // 整页导航到附件（地址栏直接打开一个 .mp4 / .zip）时，WebView 只会留下
            // 一张白纸——用户既不知道下载有没有开始，也不知道去哪看进度。
            // 这里给一张明确的落点页，成本很低，但把"是不是坏了"变成"已在下载"。
            if (webView.url == url) {
                runCatching {
                    webView.stopLoading()
                    // baseURL 用原地址：地址栏继续显示"下载来自哪里"，而不是 about:blank。
                    webView.loadDataWithBaseURL(url, downloadHintPage(url), "text/html", "utf-8", url)
                }
            }
        }
        return webView
    }

    /** 整页导航触发下载后的替代页面。跟随 App 自己的夜间开关，不依赖系统主题。 */
    private fun downloadHintPage(url: String): String {
        val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "文件" }
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        // App 内的夜间模式不改系统主题，所以不能只靠 prefers-color-scheme：
        // 否则夜里点下载会弹出一张大白页。这里由 forceDark 直接决定配色。
        val dark = forceDark
        val bodyBg = if (dark) "#151029" else "#F4F6FC"
        val fg = if (dark) "#EDEAFF" else "#1B1B2F"
        val cardBg = if (dark) "rgba(255,255,255,.07)" else "rgba(255,255,255,.74)"
        val shadow = if (dark) "0 14px 36px rgba(0,0,0,.45)" else "0 14px 36px rgba(60,50,130,.14)"
        return """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <style>
              :root{color-scheme:${if (dark) "dark" else "light"}}
              body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;
                   font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;
                   background:$bodyBg;color:$fg}
              .card{padding:30px 24px;border-radius:24px;max-width:84%;text-align:center;
                    background:$cardBg;box-shadow:$shadow}
              .badge{width:56px;height:56px;border-radius:18px;margin:0 auto 18px;
                     background:linear-gradient(135deg,#5B6CFF,#8B5CF6);display:flex;
                     align-items:center;justify-content:center;font-size:26px;color:#fff}
              h1{font-size:18px;margin:0 0 10px;font-weight:600}
              p{font-size:13px;line-height:1.7;margin:0;opacity:.72;word-break:break-all}
            </style></head><body>
              <div class="card">
                <div class="badge">&#8595;</div>
                <h1>已开始下载</h1>
                <p>$name</p>
                <p style="margin-top:10px">进度见通知栏，或从「菜单 &rarr; 我的下载」查看。</p>
              </div>
            </body></html>
        """.trimIndent()
    }

    private fun update(tabId: String, block: (BrowserTab) -> BrowserTab) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) block(it) else it }
    }

    private fun ArrayDeque<BrowserEngine.ClosedEntry>.removeLastOrNull_(): BrowserEngine.ClosedEntry? =
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
        /** 常见广告/统计域名关键词（简单规则，后续 Chromium 网络栈可换 EasyList）。 */
        val AD_HOST_KEYWORDS = listOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "adservice.google", "adnxs.com", "adsrvr.org",
            "criteo.com", "criteo.net", "moatads.com", "scorecardresearch.com",
            "adview.cn", "admaster.com.cn", "tanx.com", "g.umeng.com", "umeng.co",
            "clicki.cn", "cnzz.com", "baidu.com/hm.js", "pos.baidu.com",
            "googletagmanager.com", "taboola.com", "outbrain.com", "ad.coln.cn",
        )

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

        /**
         * 阅读模式正文抽取。
         *
         * 做法：
         * 1) 给所有候选容器打分（段落文本量 × 非链接比例 ÷ 嵌套复杂度），取分最高者作为正文根；
         * 2) 从正文根按文档顺序收集块级文本，跳过脚本/导航/侧栏/隐藏节点；
         * 3) 按 class/id 关键字过滤广告、分享、评论、相关阅读等样板块；
         * 4) 文本去重，并丢掉与 `document.title` 重复的首个标题（阅读页自己会渲染标题）。
         *
         * 小标题用 `## ` 前缀返回，交给阅读页按标题样式渲染。
         */
        const val READER_SCRIPT = """
        (function(){
          try {
            var BOILER = /(^|[-_\s])(ad|ads|adv|adsense|advert|advertisement|sponsor|promo|banner|share|sharebar|comment|comments|related|recommend|sidebar|side|footer|nav|navbar|menu|breadcrumb|cookie|popup|modal|subscribe|newsletter|toolbar|pagination)([-_\s]|\d|${'$'})/i;
            var SKIP = {SCRIPT:1,STYLE:1,NOSCRIPT:1,IFRAME:1,FORM:1,BUTTON:1,NAV:1,HEADER:1,FOOTER:1,ASIDE:1,SELECT:1,TEXTAREA:1,SVG:1,CANVAS:1};

            function boiler(el) {
              var cur = el, depth = 0;
              while (cur && cur !== document.body && depth < 5) {
                var cls = (cur.className && cur.className.toString) ? cur.className.toString() : '';
                var id = cur.id || '';
                if (BOILER.test(cls) || BOILER.test(id)) return true;
                cur = cur.parentElement; depth++;
              }
              return false;
            }
            function hidden(el) {
              if (el.getClientRects().length === 0) return true;
              var st = window.getComputedStyle(el);
              return st.display === 'none' || st.visibility === 'hidden' || parseFloat(st.opacity || '1') < 0.05;
            }

            // 1) 选正文根
            var best = null, bestScore = 0;
            var cands = document.querySelectorAll('article, main, section, div');
            for (var i = 0; i < cands.length; i++) {
              var c = cands[i];
              if (SKIP[c.tagName] || boiler(c) || hidden(c)) continue;
              var ps = c.querySelectorAll('p');
              if (!ps.length) continue;
              var textLen = 0;
              for (var j = 0; j < ps.length; j++) textLen += (ps[j].innerText || '').trim().length;
              if (textLen < 120) continue;
              var all = (c.innerText || '').length || 1;
              var linkLen = 0, as = c.querySelectorAll('a');
              for (var k = 0; k < as.length; k++) linkLen += (as[k].innerText || '').length;
              var density = Math.min(linkLen / all, 0.9);
              var complexity = c.querySelectorAll('div').length;
              var score = textLen * (1 - density) / (1 + Math.log(complexity + 1));
              if (score > bestScore) { bestScore = score; best = c; }
            }
            var root = best || document.body;

            // 2) 收集块级文本
            var out = [], seen = {};
            var title = (document.title || '').trim();
            var blocks = root.querySelectorAll('h1, h2, h3, h4, p, li, blockquote, pre');
            for (var m = 0; m < blocks.length; m++) {
              var el = blocks[m];
              var tag = el.tagName;
              if (SKIP[tag] || boiler(el) || hidden(el)) continue;
              if (el.querySelector('p, li, blockquote')) continue;   // 只取最内层，避免重复
              var t = (el.innerText || '').replace(/[ \t]+\n/g, '\n').trim();
              if (!t) continue;
              var isHeading = (tag === 'H1' || tag === 'H2' || tag === 'H3' || tag === 'H4');
              if (!isHeading && t.length < 12) continue;
              if (isHeading && t.length <= 60 && (t === title || seen[t])) continue;  // 标题已在页面头部
              if (seen[t]) continue;
              seen[t] = 1;
              out.push(isHeading ? ('## ' + t) : t);
            }

            var text = out.join('\n\n');
            if (text.length < 400) {
              text = (root.innerText || document.body.innerText || '')
                .replace(/\n{3,}/g, '\n\n').trim();
            }
            return JSON.stringify({title: title, text: text.slice(0, 20000)});
          } catch (e) { return null; }
        })();
        """
    }
}
