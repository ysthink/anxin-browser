package com.anxin.browser

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anxin.browser.design.AnxinTheme
import com.anxin.browser.nav.AnxinNavHost

/** 跨入口（桌面快捷方式 / 外部链接）待打开的地址。 */
object PendingLink {
    val url = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    /** 由下载通知点击置位，导航层消费后跳转下载页。 */
    val openDownloads = kotlinx.coroutines.flow.MutableStateFlow(false)
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val KEY_NOTIF_ASKED = "notif_permission_asked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureIntent(intent)
        // 预热服务定位器，内核使用 Activity Context 以支持 JS 对话框 / 权限回调。
        val graph = AppGraph.get(this)
        setContent {
            // 夜间模式是用户设置而非系统设置，必须在这里显式收集才能驱动整套 UI 配色。
            val nightMode by graph.settings.nightMode.collectAsStateWithLifecycle(initialValue = false)
            LaunchedEffect(nightMode) {
                // 首帧后把启动图换成纯色：用户切换夜间模式时也能立刻跟上，
                // 避免切换瞬间从深色页面退回亮色窗口底色。
                // 冷启动阶段这段代码尚未执行，窗口仍显示 splash_window（深底 + 标识）。
                window.setBackgroundDrawable(
                    ColorDrawable(if (nightMode) 0xFF0B0A1F.toInt() else 0xFFE9F0FF.toInt())
                )
            }
            AnxinTheme(darkTheme = nightMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    AnxinNavHost()
                }
            }

            // Android 13+ 的通知权限。没有它前台服务照样能跑，但下载进度条不会出现，
            // 用户会觉得"下到哪了完全不知道"。只在首次启动问一次，拒绝后不再打扰。
            val notifPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val prefs = getSharedPreferences("anxin_prefs", MODE_PRIVATE)
                    val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!granted && !prefs.getBoolean(KEY_NOTIF_ASKED, false)) {
                        prefs.edit().putBoolean(KEY_NOTIF_ASKED, true).apply()
                        notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureIntent(intent)
    }

    private fun captureIntent(intent: Intent?) {
        val fromExtra = intent?.getStringExtra("open_url")
        val fromView = if (intent?.action == Intent.ACTION_VIEW) intent.dataString else null
        (fromExtra ?: fromView)?.let { PendingLink.url.value = it }
        // 点下载通知进来：直接把用户送到下载页，而不是停在首页让他自己找。
        if (intent?.getBooleanExtra(
                com.anxin.browser.download.DownloadService.EXTRA_OPEN_DOWNLOADS,
                false,
            ) == true
        ) {
            PendingLink.openDownloads.value = true
            intent.removeExtra(com.anxin.browser.download.DownloadService.EXTRA_OPEN_DOWNLOADS)
        }
    }
}
