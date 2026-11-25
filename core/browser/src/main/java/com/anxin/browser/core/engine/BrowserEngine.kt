package com.anxin.browser.core.engine

import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.model.MediaResource
import kotlinx.coroutines.flow.StateFlow

/**
 * 内核统一接口。
 *
 * 上层（UI / 标签 / 下载 / 嗅探）只依赖此接口，不依赖具体内核实现。
 * 当前提供 [WebViewEngine] 作为第一阶段实现；接入 Titanium / Chromium 时，
 * 只需新增一个实现类（内部通过 JNI / AndroidView 承载 CompositorView），
 * 其余业务代码无需改动。
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

    /** 打印适配器（android.print.PrintDocumentAdapter）。 */
    fun createPrintAdapter(title: String): Any?

    /** 保存离线网页（MHTML），返回文件路径，失败返回 null。 */
    fun saveOfflinePage(): String?

    // ---- 标签恢复 ----

    /** 恢复最近关闭的标签（打开新标签），成功返回该标签。 */
    fun restoreLastClosed(): BrowserTab?
    val closedTabCount: Int

    fun clearMediaResources()
    fun clearBrowsingData()
    fun destroy()
}
