package com.anxin.browser.feature.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassSegment
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    closedTabCount: Int,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognito: () -> Unit,
    onClearAll: () -> Unit,
    onRestore: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    val normalTabs = remember(tabs) { tabs.filterNot { it.isIncognito } }
    val incognitoTabs = remember(tabs) { tabs.filter { it.isIncognito } }
    val recentTabs = remember(tabs) { tabs.sortedByDescending { it.lastActiveAt } }
    var segment by remember { mutableIntStateOf(0) }

    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Rounded.ArrowBack, "返回", size = 36.dp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                PageTitle(title = "标签页", count = tabs.size)
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Add, "新建标签页", size = 36.dp, onClick = onNewTab)
            }

            // 三分模式：最近 / 窗口 / 无痕（紫色高亮）
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassSegment("最近 (${recentTabs.size})", segment == 0) { segment = 0 }
                GlassSegment("窗口 (${normalTabs.size})", segment == 1) { segment = 1 }
                GlassSegment("无痕 (${incognitoTabs.size})", segment == 2) { segment = 2 }
            }

            Spacer(Modifier.height(14.dp))

            val shown = when (segment) {
                0 -> recentTabs
                1 -> normalTabs
                else -> incognitoTabs
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        selected = tab.id == activeTabId,
                        onClick = { onSelectTab(tab.id) },
                        onClose = { onCloseTab(tab.id) },
                    )
                }
                if (shown.isEmpty()) {
                    item { EmptyTabs(incognito = segment == 2) { if (segment == 2) onNewIncognito() else onNewTab() } }
                }
                if (shown.isNotEmpty()) item {
                    Spacer(Modifier.height(6.dp))
                    BottomOpsRow(
                        canClear = tabs.isNotEmpty(),
                        canRestore = closedTabCount > 0,
                        onClearAll = onClearAll,
                        onNew = { if (segment == 2) onNewIncognito() else onNewTab() },
                        onRestore = onRestore,
                    )
                }
            }
        }
        AnxinActionBar(
            actions = actions,
            tabCount = tabs.size,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** 底部操作：清空全部 / 新建 + / 恢复上一步。 */
@Composable
private fun BottomOpsRow(
    canClear: Boolean,
    canRestore: Boolean,
    onClearAll: () -> Unit,
    onNew: () -> Unit,
    onRestore: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 26.dp, alpha = 0.5f, shadowElevation = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OpsItem(Icons.Rounded.DeleteSweep, "清空全部", enabled = canClear, onClick = onClearAll)
        OpsDivider()
        OpsItem(Icons.Rounded.Add, "新建", enabled = true, accent = true, onClick = onNew)
        OpsDivider()
        OpsItem(Icons.Rounded.RestoreFromTrash, "恢复上一步", enabled = canRestore, onClick = onRestore)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.OpsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .weight(1f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                accent -> palette.brand
                enabled -> palette.inkSecondary
                else -> palette.inkTertiary.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                accent -> palette.brandDeep
                enabled -> palette.inkSecondary
                else -> palette.inkTertiary.copy(alpha = 0.5f)
            },
        )
    }
}

@Composable
private fun OpsDivider() {
    Box(
        Modifier
            .width(0.8.dp)
            .height(22.dp)
            .background(AnxinPalette.ink.copy(alpha = 0.08f)),
    )
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = AnxinPalette
    val host = Urls.prettyHost(tab.url).ifBlank { "新标签页" }
    val accent = when {
        tab.isIncognito -> Color(0xFF8A6BFF)
        host.contains("baidu") -> Color(0xFF2B34E8)
        host.contains("mi.com") -> Color(0xFFFF6900)
        host.contains("bilibili") -> Color(0xFFFB7299)
        else -> palette.brand
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                radius = 22.dp,
                alpha = if (selected) 0.72f else 0.5f,
                borderAlpha = if (selected) 0.95f else 0.6f,
                shadowElevation = if (selected) 20.dp else 10.dp,
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(
                        if (tab.isIncognito) Color(0xFF6B4BD6).copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.75f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (tab.isIncognito) Icons.Rounded.Lock else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tab.title.ifBlank { "新标签页" },
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (tab.isIncognito) "无痕标签 · $host" else tab.url.ifBlank { "about:blank" },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭标签",
                    tint = palette.inkSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.92f),
                            accent.copy(alpha = 0.10f),
                        )
                    )
                ),
        ) {
            Text(
                text = host.take(1).uppercase(),
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = accent.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.Center),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            ) {
                Text(
                    host,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink.copy(alpha = 0.75f),
                )
                Text(
                    if (tab.isLoading) "加载中 ${tab.progress}%" else "已加载完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.brand.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("当前", style = MaterialTheme.typography.labelSmall, color = palette.brandDeep)
                }
            }
        }
    }
}

@Composable
private fun EmptyTabs(incognito: Boolean, onCreate: () -> Unit) {
    val palette = AnxinPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .glassPanel(radius = 48.dp, alpha = 0.5f, shadowElevation = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (incognito) Icons.Rounded.Lock else Icons.Rounded.Tab,
                contentDescription = null,
                tint = palette.brand,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (incognito) "还没有无痕标签" else "还没有打开的网页",
            style = MaterialTheme.typography.titleMedium,
            color = palette.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (incognito) "无痕标签关闭后不会留下任何记录" else "点击下方按钮开始浏览，或从首页搜索",
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkTertiary,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .height(44.dp)
                .glassPanel(radius = 22.dp, alpha = 0.6f, shadowElevation = 12.dp)
                .clickable(onClick = onCreate)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.brand, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (incognito) "新建无痕标签" else "新建标签页",
                style = MaterialTheme.typography.titleSmall,
                color = palette.brandDeep,
            )
        }
    }
}
