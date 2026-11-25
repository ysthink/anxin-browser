package com.anxin.browser.design

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 控制系统栏图标明暗，使沉浸式页面（播放器 / 无痕浏览）在深色背景上显示白色图标。
 * 传入 [lightIcons] = true 表示「亮色图标」。
 */
@Composable
fun SystemBarsAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(lightIcons) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !lightIcons
        controller?.isAppearanceLightNavigationBars = !lightIcons
        onDispose {
            if (previousStatus != null) controller?.isAppearanceLightStatusBars = previousStatus
            if (previousNav != null) controller?.isAppearanceLightNavigationBars = previousNav
        }
    }
}
