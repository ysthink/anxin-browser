package com.anxin.browser

import android.content.Context
import com.anxin.browser.core.download.DownloadRepository
import com.anxin.browser.core.engine.BrowserEngine
import com.anxin.browser.core.engine.ChromiumEngineHooks
import com.anxin.browser.core.engine.WebViewEngine
import com.anxin.browser.data.BookmarksHistoryRepo
import com.anxin.browser.data.HistoryStore
import com.anxin.browser.data.SessionStore
import com.anxin.browser.data.SettingsStore
import com.anxin.browser.data.ShortcutStore
import com.anxin.browser.data.extension.ScriptExtensionBridge
import kotlinx.coroutines.launch

/**
 * 轻量服务定位器。规模变大后替换为 Hilt，接口保持不变。
 *
 * 内核选择由 `-PuseChromiumKernel=true` 控制（见 BuildConfig.USE_CHROMIUM_KERNEL）。
 * ChromiumEngine 通过反射创建，默认构建不依赖可选 AAR。
 */
class AppGraph private constructor(context: Context) {
    val downloads: DownloadRepository = DownloadRepository(context.applicationContext)
    val settings: SettingsStore = SettingsStore(context.applicationContext)
    val bookmarksRepo: BookmarksHistoryRepo = BookmarksHistoryRepo(context.applicationContext)
    val history: HistoryStore = HistoryStore(context.applicationContext)
    val session: SessionStore = SessionStore(context.applicationContext)
    val shortcuts: ShortcutStore = ShortcutStore(context.applicationContext)
    val extensions: ScriptExtensionBridge = ScriptExtensionBridge(context.applicationContext)

    val engine: BrowserEngine = if (BuildConfig.USE_CHROMIUM_KERNEL) {
        createChromiumEngine(context).also { engine ->
            (engine as? ChromiumEngineHooks)?.let { hooks ->
                hooks.setVisitListener { title, url -> bookmarksRepo.record(title, url) }
                hooks.setPageScriptProvider { url -> extensions.scriptsFor(url) }
            }
        }
    } else {
        WebViewEngine(
            context = context.applicationContext,
            downloads = downloads,
            onVisit = { title, url -> bookmarksRepo.record(title, url) },
        ).also { engine ->
            // 类扩展注入：内核按 URL 向桥层要脚本
            engine.pageScriptProvider = { url -> extensions.scriptsFor(url) }
        }
    }

    init {
        // 一次性把旧 DataStore 书签/历史迁入 Room（幂等：IGNORE 冲突 + 完成后清旧键）
        bookmarksRepo.scope.launch {
            bookmarksRepo.migrateFrom(settings, history)
        }
    }

    private fun createChromiumEngine(context: Context): BrowserEngine {
        val factory = Class.forName("com.anxin.browser.core.engine.ChromiumEngine")
            .getMethod(
                "create",
                Context::class.java,
                DownloadRepository::class.java,
            )
        return factory.invoke(null, context.applicationContext, downloads) as BrowserEngine
    }

    companion object {
        @Volatile
        private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context).also { instance = it }
            }
    }
}
