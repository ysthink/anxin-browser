package com.anxin.browser

import android.content.Context
import com.anxin.browser.core.download.DownloadRepository
import com.anxin.browser.core.engine.WebViewEngine
import com.anxin.browser.data.ExtensionStore
import com.anxin.browser.data.HistoryStore
import com.anxin.browser.data.SessionStore
import com.anxin.browser.data.SettingsStore
import com.anxin.browser.data.ShortcutStore

/**
 * 轻量服务定位器。规模变大后替换为 Hilt，接口保持不变。
 */
class AppGraph private constructor(context: Context) {
    val downloads: DownloadRepository = DownloadRepository(context.applicationContext)
    val history: HistoryStore = HistoryStore(context.applicationContext)
    val session: SessionStore = SessionStore(context.applicationContext)
    val engine: WebViewEngine = WebViewEngine(
        context = context.applicationContext,
        downloads = downloads,
        onVisit = { title, url -> history.record(title, url) },
    )
    val settings: SettingsStore = SettingsStore(context.applicationContext)
    val shortcuts: ShortcutStore = ShortcutStore(context.applicationContext)
    val extensions: ExtensionStore = ExtensionStore()

    companion object {
        @Volatile
        private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context).also { instance = it }
            }
    }
}
