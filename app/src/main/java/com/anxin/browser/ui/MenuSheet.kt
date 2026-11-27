package com.anxin.browser.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.anxinDivider
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.anxinSurface
import com.anxin.browser.design.isAnxinDark
import com.anxin.browser.data.MenuEntry

/** 菜单面板动作全集：id -> (文案, 图标)。 */
object MenuActions {
    data class Spec(val id: String, val label: String, val icon: ImageVector)

    val all: List<Spec> = listOf(
        Spec("bookmark", "书签", Icons.Rounded.Bookmark),
        Spec("history", "历史", Icons.Rounded.History),
        Spec("downloads", "我的下载", Icons.Rounded.Download),
        Spec("videos", "我的视频", Icons.Rounded.VideoLibrary),
        Spec("night", "夜间模式", Icons.Rounded.DarkMode),
        Spec("incognito", "无痕模式", Icons.Rounded.Lock),
        Spec("addBookmark", "添加书签", Icons.Rounded.BookmarkAdd),
        Spec("refresh", "刷新", Icons.Rounded.Refresh),
        Spec("exit", "退出", Icons.Rounded.ExitToApp),
        Spec("reading", "阅读模式", Icons.Rounded.MenuBook),
        Spec("images", "看图模式", Icons.Rounded.Image),
        Spec("fullscreen", "全屏模式", Icons.Rounded.Fullscreen),
        Spec("desktop", "电脑版", Icons.Rounded.Computer),
        Spec("extensions", "扩展程序", Icons.Rounded.Extension),
        Spec("sniff", "资源嗅探", Icons.Rounded.Theaters),
        Spec("translate", "网页翻译", Icons.Rounded.Language),
        Spec("addToDesktop", "加到桌面", Icons.Rounded.AddToHomeScreen),
        Spec("find", "页面查找", Icons.Rounded.FindInPage),
        Spec("saveOffline", "离线保存", Icons.Rounded.SaveAlt),
        Spec("textSize", "文字大小", Icons.Rounded.TextFields),
        Spec("admark", "广告标记", Icons.Rounded.Block),
        Spec("print", "网页打印", Icons.Rounded.Print),
        Spec("zoom", "网页缩放", Icons.Rounded.ZoomIn),
    )

    fun spec(id: String): Spec? = all.firstOrNull { it.id == id }
}

/**
 * 浏览菜单面板：
 * - 常态：按用户布局渲染可见项（4 列网格，行序跟随存储顺序）；
 * - 编辑态：全部项列表，支持上移/下移排序与显示/隐藏开关；
 * - 布局由 SettingsStore.menuLayout 持久化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    layout: List<MenuEntry>,
    editing: Boolean,
    activeIds: Set<String> = emptySet(),
    commonExtensions: List<com.anxin.browser.data.extension.UserExtension> = emptyList(),
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onEditToggle: () -> Unit,
    onMove: (index: Int, up: Boolean) -> Unit,
    onToggleVisible: (id: String, visible: Boolean) -> Unit,
    onToggleCommonExtension: (id: String, enabled: Boolean) -> Unit = { _, _ -> },
    onShare: () -> Unit,
    onSettings: () -> Unit,
) {
    val palette = AnxinPalette
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 10.dp)
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(30.dp),
                    clip = false,
                    ambientColor = Color(0xFF1B2A55).copy(alpha = 0.28f),
                    spotColor = Color(0xFF1B2A55).copy(alpha = 0.32f),
                )
                .clip(RoundedCornerShape(30.dp))
                .background(
                    anxinSurface()
                )
                .border(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        if (isAnxinDark) {
                            listOf(palette.hairline, Color(0xFF3E3568))
                        } else {
                            listOf(Color.White.copy(alpha = 0.95f), Color(0xFFE3EAF9))
                        },
                    ),
                    shape = RoundedCornerShape(30.dp),
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // ---- 账号区 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFF9FC0FF), Color(0xFF4A6BFF), Color(0xFF7B5CFF)),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("石", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("安心用户", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Text("轻量浏览版 · 本地数据", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(anxinField())
                        .clickable(onClick = onShare)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "分享", tint = palette.brand, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("分享", style = MaterialTheme.typography.labelMedium, color = palette.ink, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(anxinField())
                        .clickable(onClick = onSettings)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = palette.brand, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("设置", style = MaterialTheme.typography.labelMedium, color = palette.ink, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (!editing) {
                // ---- 常态：可见项网格 ----
                val visible = layout.filter { it.visible }.mapNotNull { entry -> MenuActions.spec(entry.id) }
                visible.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // 每格固定 1/5 行宽：不足五个的尾行靠左对齐、列与上行对齐，不再居中悬空。
                        row.forEach { spec ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                MenuIcon(
                                    label = spec.label,
                                    icon = spec.icon,
                                    active = spec.id in activeIds,
                                ) {
                                    onDismiss()
                                    onAction(spec.id)
                                }
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // ---- 常用扩展快捷行（设置里开启「菜单显示常用扩展」后出现）----
                // 只展示用户固定（pinned）的扩展：常用性由用户自己定义，
                // 不固定 = 不打扰。点开关直接启停，长路径变短路径。
                if (commonExtensions.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "常用扩展",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkTertiary,
                    )
                    Spacer(Modifier.height(6.dp))
                    commonExtensions.forEach { ext ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(anxinField())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ext.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (ext.enabled) palette.ink else palette.inkTertiary,
                                    maxLines = 1,
                                )
                                Text(
                                    if (ext.enabled) "已启用" else "已停用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.inkTertiary,
                                )
                            }
                            Switch(
                                checked = ext.enabled,
                                onCheckedChange = { onToggleCommonExtension(ext.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.brand,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = anxinDivider(),
                                ),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            } else {
                // ---- 编辑态：全项排序 + 显隐 ----
                Text(
                    "上下箭头排序 · 开关控制显隐",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    itemsIndexed(layout, key = { _, e -> e.id }) { index, entry ->
                        val spec = MenuActions.spec(entry.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (entry.visible) anxinField() else anxinSurface())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                spec?.icon ?: Icons.Rounded.Tab,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                spec?.label ?: entry.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (entry.visible) palette.ink else palette.inkTertiary,
                                modifier = Modifier.weight(1f),
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = index > 0) { onMove(index, true) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "上移",
                                        tint = if (index > 0) palette.inkSecondary else palette.inkTertiary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = index < layout.lastIndex) { onMove(index, false) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "下移",
                                        tint = if (index < layout.lastIndex) palette.inkSecondary else palette.inkTertiary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Switch(
                                checked = entry.visible,
                                onCheckedChange = { onToggleVisible(entry.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.brand,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = anxinDivider(),
                                ),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(anxinField())
                    .border(1.dp, anxinDivider(), RoundedCornerShape(23.dp))
                    .clickable(onClick = onEditToggle),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (editing) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = palette.brand,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (editing) "完成自定义" else "自定义布局",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MenuIcon(
    label: String,
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Column(
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = if (active) 8.dp else 5.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = palette.brand.copy(alpha = if (active) 0.45f else 0.16f),
                        spotColor = palette.brand.copy(alpha = if (active) 0.5f else 0.18f),
                    )
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(palette.brandSoft, palette.brand, palette.violet),
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(anxinField(), anxinSurface()),
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (active) palette.hairline else anxinDivider(),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (active) palette.onAccent else palette.brand,
                    modifier = Modifier.size(21.dp),
                )
            }
            // 开启状态角标
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(anxinSurface())
                        .border(1.2.dp, palette.brand, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "已开启",
                        tint = palette.brand,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (active) palette.brandDeep else palette.ink.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** 系统分享面板的辅助方法，供导航层调用。 */
fun shareUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享链接")) }
}
