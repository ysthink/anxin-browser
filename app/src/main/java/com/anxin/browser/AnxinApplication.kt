package com.anxin.browser

import android.app.Application
import com.anxin.browser.core.model.DownloadStatus
import com.anxin.browser.data.update.UpdateCenter
import com.anxin.browser.download.DownloadService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AnxinApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // 有任务在跑就把进程抬到前台优先级，避免用户切到别的 App 后被回收导致下载中断。
        // 只负责"拉起"：没有活跃任务时服务自己在 onStartCommand 的观察里 stopSelf()。
        val downloads = AppGraph.get(this).downloads
        scope.launch {
            downloads.items
                .map { items ->
                    items.any {
                        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PENDING
                    }
                }
                .distinctUntilChanged()
                .collect { active -> if (active) DownloadService.start(this@AnxinApplication) }
        }

        // 应用内更新：启动时静默检查（24h 节流），发现新版本只在关于页提示
        UpdateCenter.autoCheckIfNeeded(this)
    }
}
