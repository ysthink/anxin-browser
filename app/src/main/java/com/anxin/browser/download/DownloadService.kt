package com.anxin.browser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.anxin.browser.AppGraph
import com.anxin.browser.MainActivity
import com.anxin.browser.R
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 下载前台服务。
 *
 * 真正的传输仍在 [com.anxin.browser.core.download.DownloadRepository] 里跑，
 * 这个服务只负责两件事：
 * 1) 把进程提到前台优先级，用户切走或锁屏后不被系统回收；
 * 2) 在通知栏给出总进度，并提供一键回到下载页的入口。
 *
 * 所有任务结束就 `stopSelf()`——没有任务还挂着常驻通知是纯粹的骚扰。
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须尽早进入前台：Android 要求 startForegroundService 后 5 秒内调用 startForeground。
        promoteToForeground(null)

        if (watcher == null) {
            watcher = scope.launch {
                AppGraph.get(this@DownloadService).downloads.items.collectLatest { items ->
                    val active = items.filter {
                        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PENDING
                    }
                    if (active.isEmpty()) {
                        stopSelf()
                    } else {
                        promoteToForeground(active)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(active: List<DownloadItem>?) {
        val notification = buildNotification(active)
        runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        }
    }

    private fun buildNotification(active: List<DownloadItem>?): Notification {
        val list = active.orEmpty()
        val title = when {
            list.isEmpty() -> "正在准备下载"
            list.size == 1 -> "正在下载 ${list.first().fileName}"
            else -> "正在下载 ${list.size} 项"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())

        val single = list.singleOrNull()
        if (single != null && single.status == DownloadStatus.RUNNING) {
            val known = single.totalBytes > 0
            val percent = if (known) {
                (single.downloadedBytes * 100 / single.totalBytes).coerceIn(0, 100).toInt()
            } else {
                0
            }
            builder.setContentText(
                if (known) {
                    "$percent% · ${formatBytes(single.downloadedBytes)} / ${formatBytes(single.totalBytes)}"
                } else {
                    formatBytes(single.downloadedBytes)
                }
            )
            // 总大小未知时给不确定进度条，避免出现"永远 0%"的死条。
            builder.setProgress(if (known) 100 else 0, percent, !known)
        } else if (list.isNotEmpty()) {
            builder.setContentText("${list.size} 个任务进行中").setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_DOWNLOADS, true)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "下载",
                // 下载进度属于"用户已经知道正在发生"的事，不需要提示音。
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示下载进度"
                setShowBadge(false)
            },
        )
    }

    override fun onDestroy() {
        watcher?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "anxin_downloads"
        private const val NOTIFICATION_ID = 0x0D1
        const val EXTRA_OPEN_DOWNLOADS = "open_downloads"

        /** 有活跃任务时拉起前台服务；已在运行则什么也不做。 */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
            // Android 12+ 在后台不允许启动前台服务（ForegroundServiceStartNotAllowedException）。
            // 此时下载仍在进程内继续，只是少了保活，不该让用户看到崩溃。
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, DownloadService::class.java)) }
        }

        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1L shl 30 -> String.format("%.1f GB", bytes / (1L shl 30).toDouble())
            bytes >= 1L shl 20 -> String.format("%.1f MB", bytes / (1L shl 20).toDouble())
            bytes >= 1L shl 10 -> String.format("%.0f KB", bytes / (1L shl 10).toDouble())
            else -> "$bytes B"
        }
    }
}
