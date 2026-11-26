package com.anxin.browser.core.engine

import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.TabSession
import kotlinx.coroutines.flow.StateFlow

/**
 * 内核统一接口。
 *
 * 上层（UI / 标签 / 下载 / 嗅探）只依赖此接口，不依赖具体内核实现。
 * 当前提供 [WebViewEngine] 作为第一阶段实现；
 * M3 阶段二提供 ChromiumEngine（AwContents 直嵌，自带 Chromium 内核随 APK 分发，
 * 不依赖系统 WebView Provider），仅 AppGraph 一行切换。
 */
interface BrowserEngine {

    val tabs: StateFlow<List<BrowserTab>>
    val activeTabId: StateFlow<String?>
    val mediaResources: StateFlow<List<MediaResource>>

    fun createTab(url: String = "about:blank", incognito: Boolean = false): BrowserTab
    fun closeTab(tabId: String)
    fun closeAllIncognito()
    fun selectTab(tabId: String)

    /** 当前标签页访问器（实现层类型，UI 用它做 AndroidView 挂载）。 */
    fun activeContentView(): Any?

    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun stopLoading()

    // ---- 网页浏览设置 / 页面操作 ----

    /** 页面文字缩放百分比（50 ~ 200）。 */
    fun setTextZoom(percent: Int)

    /** 访问电脑版：切换 UA 并刷新当前页。 */
    fun setDesktopMode(enabled: Boolean)

    /** 夜间模式：强制暗色渲染。 */
    fun setForceDark(enabled: Boolean)

    /** 页面查找，返回首个匹配数（0 表示不可用）。 */
    fun findInPage(text: String): Int

    /** 广告过滤开关（网络层拦截广告域名请求）。 */
    fun setAdBlock(enabled: Boolean)

    /** 阅读模式：抽取页面正文，回调 JSON（title/text），失败回调 null。 */
    fun extractReadablePage(callback: (String?) -> Unit)

    /** 打印适配器（android.print.PrintDocumentAdapter）。 */
    fun createPrintAdapter(title: String): Any?

    /** 保存离线网页（MHTML），返回文件路径，失败返回 null。 */
    fun saveOfflinePage(): String?

    // ---- 标签恢复 ----

    /** 恢复最近关闭的标签（打开新标签），成功返回该标签。 */
    fun restoreLastClosed(): BrowserTab?
    val closedTabCount: Int

    /** 进程重启后的会话恢复：重建标签但只加载 [activeIndex] 对应页，其余懒加载。 */
    fun restoreSession(sessions: List<TabSession>, activeIndex: Int): Boolean

    fun clearMediaResources()
    fun clearBrowsingData()
    fun destroy()
}
