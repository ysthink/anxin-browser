package com.anxin.browser

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
}

class MainActivity : ComponentActivity() {

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
                // 避免切换瞬间从深色页面退回亮色窗口底色。
                window.setBackgroundDrawable(
                    ColorDrawable(if (nightMode) 0xFF0B0A1F.toInt() else 0xFFE9F0FF.toInt())
                )
            }
            AnxinTheme(darkTheme = nightMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    AnxinNavHost()
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
    }
}
