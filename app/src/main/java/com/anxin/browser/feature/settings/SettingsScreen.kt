package com.anxin.browser.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.anxin.browser.design.AnxinDialogContainer
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinDivider
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.IconBubble
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

/** 设置中心主页面：常规 / 辅助 / 其他 三组入口。 */
@Composable
fun SettingsScreen(
    signedIn: Boolean,
    adBlockEnabled: Boolean,
    doNotTrack: Boolean,
    exitConfirm: Boolean,
    searchEngine: String,
    textZoom: Int,
    showEngineBar: Boolean,
    nightMode: Boolean,
    onEngineBarChange: (Boolean) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onExitConfirmChange: (Boolean) -> Unit,
    onAdBlockChange: (Boolean) -> Unit,
    onDoNotTrackChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenSearchSettings: () -> Unit,
    onOpenWebSettings: () -> Unit,
    onOpenPlaySettings: () -> Unit,
    onOpenSearchEngine: () -> Unit,
    onOpenIncognito: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenAbout: () -> Unit,
    onResetDefaults: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    var showResetDialog by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageTitle(title = "设置")
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))

            // 账号卡
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .height(64.dp)
                    .glassPanel(radius = 22.dp, alpha = 0.45f, shadowElevation = 8.dp)
                    .clickable(onClick = onSignIn)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF9FC0FF), Color(0xFF4A6BFF), Color(0xFF7B5CFF)),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("石", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (signedIn) "安心用户" else "登录账号",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                    )
                    Text(
                        if (signedIn) "已同步书签、历史与设置" else "同步书签、历史、设置等",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkTertiary,
                    )
                }
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.inkTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ---- 常规 ----
            GroupCard("常规") {
                SwitchEntry(
                    Icons.Rounded.DarkMode,
                    palette.violet,
                    "夜间模式",
                    checked = nightMode,
                    onChecked = onNightModeChange,
                )
                CardDivider()
                RowEntry(Icons.Rounded.Home, Color(0xFF3D6BFF), "主页设置") { onOpenHomeSettings() }
                CardDivider()
                RowEntry(Icons.Rounded.Search, Color(0xFF2CA8FF), "搜索设置", trailing = searchEngine) { onOpenSearchSettings() }
                CardDivider()
                RowEntry(Icons.Rounded.TextFields, Color(0xFF7B5CFF), "网页浏览设置", trailing = "$textZoom%") { onOpenWebSettings() }
            }

            Spacer(Modifier.height(12.dp))

            // ---- 辅助 ----
            GroupCard("辅助") {
                SwitchEntry(
                    Icons.Rounded.Block,
                    Color(0xFF23C08A),
                    "广告过滤",
                    checked = adBlockEnabled,
                    onChecked = onAdBlockChange,
                )
                CardDivider()
                SwitchEntry(
                    Icons.Rounded.Tune,
                    Color(0xFF5B6BB5),
                    "请勿跟踪",
                    checked = doNotTrack,
                    onChecked = onDoNotTrackChange,
                )
                CardDivider()
                SwitchEntry(
                    Icons.Rounded.ExitToApp,
                    Color(0xFFFF9F43),
                    "退出前确认",
                    checked = exitConfirm,
                    onChecked = onExitConfirmChange,
                )
                CardDivider()
                SwitchEntry(
                    Icons.Rounded.Search,
                    Color(0xFF3D6BFF),
                    "显示搜索引擎切换栏",
                    checked = showEngineBar,
                    onChecked = onEngineBarChange,
                )
                CardDivider()
                RowEntry(Icons.Rounded.Bookmark, Color(0xFF2CA8FF), "书签") { onOpenBookmarks() }
                CardDivider()
                RowEntry(Icons.Rounded.History, Color(0xFF7B5CFF), "历史") { onOpenHistory() }
                CardDivider()
                RowEntry(Icons.Rounded.PlayCircle, Color(0xFF23C08A), "播放设置") { onOpenPlaySettings() }
                CardDivider()
                RowEntry(Icons.Rounded.Lock, Color(0xFF7B5CFF), "无痕模式") { onOpenIncognito() }
                CardDivider()
                RowEntry(Icons.Rounded.Extension, Color(0xFF8B6CFF), "扩展管理") { onOpenExtensions() }
                CardDivider()
                RowEntry(Icons.Rounded.Download, Color(0xFFFF9F43), "下载管理") { onOpenDownloads() }
            }

            Spacer(Modifier.height(12.dp))

            // ---- 其他 ----
            GroupCard("其他") {
                RowEntry(Icons.Rounded.Feedback, Color(0xFF2CA8FF), "意见反馈") { onOpenFeedback() }
                CardDivider()
                RowEntry(Icons.Rounded.Info, Color(0xFF6C7C99), "关于") { onOpenAbout() }
                CardDivider()
                RowEntry(Icons.Rounded.Restore, Color(0xFFFF5A5F), "恢复默认设置") { showResetDialog = true }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text("安心浏览器 v0.2.0", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
                Text("默认 WebView · 可选 Chromium 内核", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
            }

            Spacer(Modifier.height(120.dp))
        }
        AnxinActionBar(
            actions = actions,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = AnxinDialogContainer,
            title = { Text("恢复默认设置") },
            text = { Text("将清除全部应用配置（引擎、主页、菜单布局、显示设置等），书签与历史保留。确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetDefaults()
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun GroupCard(title: String, content: @Composable () -> Unit) {
    val palette = AnxinPalette
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.inkSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .glassPanel(radius = 22.dp, alpha = 0.6f, shadowElevation = 14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CardDivider() {
    Box(
        Modifier
            .padding(start = 60.dp, end = 14.dp)
            .fillMaxWidth()
            .height(0.7.dp)
            .background(anxinDivider()),
    )
}

@Composable
private fun RowEntry(
    icon: ImageVector,
    tint: Color,
    title: String,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, tint, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium,
                color = palette.inkTertiary,
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.inkTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SwitchEntry(
    icon: ImageVector,
    tint: Color,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, tint, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.ink,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = anxinDivider(),
            ),
        )
    }
}
