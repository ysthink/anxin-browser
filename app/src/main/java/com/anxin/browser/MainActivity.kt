package com.anxin.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.anxin.browser.design.AnxinTheme
import com.anxin.browser.nav.AnxinNavHost

/** 跨入口（桌面快捷方式 / 外部链接）待打开的地址。 */
object PendingLink {
    @Volatile
    var url: String? = null
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureIntent(intent)
        // 预热服务定位器，内核使用 Activity Context 以支持 JS 对话框 / 权限回调。
        AppGraph.get(this)
        setContent {
            AnxinTheme {
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
        PendingLink.url = fromExtra ?: fromView ?: PendingLink.url
    }
}
