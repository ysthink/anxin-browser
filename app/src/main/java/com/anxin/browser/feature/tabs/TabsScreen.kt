package com.anxin.browser.feature.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.core.engine.BrowserEngine
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.anxinSurfaceMuted
import com.anxin.browser.design.glassPanel
import com.anxin.browser.design.isAnxinDark
import com.anxin.browser.ui.PageTitle

private val SelectedBorder = Color(0xFF2B6BFF)
private val IncognitoAccent = Color(0xFF8A6BFF)

/** 顶部模式切换：最近 / 窗口 / 无痕。 */
@Composable
private fun ModeSegments(
    segment: Int,
    dark: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .glassPanel(
                radius = 26.dp,
                alpha = if (dark) 0.35f else 0.55f,
                shadowElevation = 8.dp,
                tint = if (dark) Color(0xFF2A2350) else Color.White,
            )
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeItem(
            icon = Icons.Rounded.Schedule,
            label = "最近",
            selected = segment == 0,
            dark = dark,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f),
        )
        ModeItem(
            icon = Icons.Rounded.Public,
            label = "窗口",
            selected = segment == 1,
            dark = dark,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f),
        )
        ModeItem(
            icon = Icons.Rounded.VisibilityOff,
            label = "无痕",
            selected = segment == 2,
            dark = dark,
            selectedColor = IncognitoAccent,
            filled = true,
            onClick = { onSelect(2) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    dark: Boolean,
    selectedColor: Color = AnxinPalette.brand,
    filled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val neutral = if (dark) Color(0xFFB9B4C7) else AnxinPalette.inkSecondary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .background(
                when {
                    selected && filled -> selectedColor
                    selected -> selectedColor.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor = when {
            selected && filled -> Color.White
            selected -> selectedColor
            else -> neutral
        }
        Icon(
            icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun TabsScreen(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    closed: List<BrowserEngine.ClosedEntry>,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognito: () -> Unit,
    onClearTabs: () -> Unit,
    onClearIncognito: () -> Unit,
    onClearClosed: () -> Unit,
    onRestoreClosed: (BrowserEngine.ClosedEntry) -> Unit,
    onRestoreLast: () -> Unit,
) {
    val palette = AnxinPalette
    val normalTabs = remember(tabs) { tabs.filterNot { it.isIncognito } }
    val incognitoTabs = remember(tabs) { tabs.filter { it.isIncognito } }
    var segment by remember { mutableIntStateOf(1) }
    // 无痕页签整体转深色氛围，让"此刻处于无痕模式"可被一眼感知。
    val incognitoUi = segment == 2
    val incognitoOverlay by animateColorAsState(
        if (incognitoUi) Color(0xFF161120) else Color.Transparent,
        animationSpec = tween(240),
        label = "incognitoOverlay",
    )

    val onCreate: () -> Unit = { if (segment == 2) onNewIncognito() else onNewTab() }

    AuroraBackground {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(incognitoOverlay)
            )
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(
                        Icons.Rounded.ArrowBack,
                        "返回",
                        size = 36.dp,
                        iconTint = if (incognitoUi) Color(0xFFE8E4F5) else palette.ink,
                        onClick = onBack,
                    )
                    Spacer(Modifier.width(12.dp))
                    PageTitle(title = "多窗口", lightText = incognitoUi)
                }

                Spacer(Modifier.height(6.dp))
                ModeSegments(
                    segment = segment,
                    dark = incognitoUi,
                    onSelect = { segment = it },
                )
                Spacer(Modifier.height(14.dp))

                Box(Modifier.weight(1f)) {
                    when (segment) {
                        0 -> RecentPane(
                            closed = closed,
                            onRestore = onRestoreClosed,
                            modifier = Modifier.fillMaxSize(),
                        )
                        1 -> WindowPane(
                            tabs = normalTabs,
                            activeTabId = activeTabId,
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> IncognitoPane(
                            tabs = incognitoTabs,
                            activeTabId = activeTabId,
                            onSelectTab = onSelectTab,
                            onCloseTab = onCloseTab,
                            onCreate = onCreate,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // 底部悬浮操作（参考图样式）：线性垃圾桶 / 实心圆 + / 撤销箭头
                    val actionTint = if (incognitoUi) Color(0xFFC9C3DD) else palette.inkSecondary
                    val plusBg = if (incognitoUi || isAnxinDark) Color.White else Color(0xFF1A1C2B)
                    val plusTint = if (incognitoUi || isAnxinDark) Color(0xFF17141F) else Color.White
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 42.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val canClear = when (segment) {
                            0 -> closed.isNotEmpty()
                            1 -> normalTabs.isNotEmpty()
                            else -> incognitoTabs.isNotEmpty()
                        }
                        BottomAction(
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = when (segment) {
                                0 -> "清空最近关闭"
                                1 -> "关闭全部窗口"
                                else -> "关闭全部无痕窗口"
                            },
                            tint = if (canClear) actionTint else actionTint.copy(alpha = 0.35f),
                            enabled = canClear,
                            onClick = {
                                when (segment) {
                                    0 -> onClearClosed()
                                    1 -> onClearTabs()
                                    else -> onClearIncognito()
                                }
                            },
                        )
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(plusBg)
                                .clickable(onClick = onCreate),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = if (segment == 2) "新建无痕窗口" else "新建窗口",
                                tint = plusTint,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        BottomAction(
                            icon = Icons.Rounded.Undo,
                            contentDescription = "恢复最近关闭",
                            tint = if (closed.isNotEmpty()) actionTint else actionTint.copy(alpha = 0.35f),
                            enabled = closed.isNotEmpty(),
                            onClick = onRestoreLast,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** 底部线性图标按钮（无底板，参考图样式）。 */
@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** 「最近」页：重新打开最近关闭的窗口。 */
@Composable
private fun RecentPane(
    closed: List<BrowserEngine.ClosedEntry>,
    onRestore: (BrowserEngine.ClosedEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    if (closed.isEmpty()) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(84.dp))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(anxinField()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Schedule, null, tint = palette.inkTertiary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "没有最近关闭的窗口",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
            )
            Text(
                "关闭的窗口会在这里保留，方便随时找回。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }
    Column(modifier) {
        Text(
            "您可以回到这里，重新打开最近关闭的窗口",
            style = MaterialTheme.typography.labelMedium,
            color = palette.inkTertiary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listItems(closed) { entry ->
                val host = Urls.prettyHost(entry.url).ifBlank { entry.url }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(radius = 18.dp, alpha = 0.42f, shadowElevation = 6.dp)
                        .clickable { onRestore(entry) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(anxinField()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            host.take(1).uppercase(),
                            color = palette.brand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title.ifBlank { host },
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entry.url,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 「窗口」页：两列窗口卡片，选中带蓝色描边。 */
@Composable
private fun WindowPane(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) {
        EmptyPane(
            icon = Icons.Rounded.Public,
            title = "还没有打开的窗口",
            hint = "点下面的 + 新建窗口，或从首页搜索开始浏览。",
            modifier = modifier,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 92.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tabs, key = { it.id }) { tab ->
            WindowCard(
                tab = tab,
                selected = tab.id == activeTabId,
                onClick = { onSelectTab(tab.id) },
                onClose = { onCloseTab(tab.id) },
            )
        }
    }
}

/** 「无痕」页：进入提示 + 无痕窗口卡片。 */
@Composable
private fun IncognitoPane(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // 无痕说明卡：深色底 + 幽灵图标，贴近常见无痕页的视觉。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF17141F))
                    .padding(horizontal = 20.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2733)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFFB9B4C7),
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "您已进入无痕浏览模式",
                    color = Color(0xFFEDEAFF),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "浏览器会保护您的隐私，您的浏览记录、网站数据和 Cookie，以及在表单中输入的信息不会在本地保存。",
                    color = Color(0xFF9C97AD),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }
        if (tabs.isEmpty()) {
            item {
                Text(
                    "还没有无痕窗口，点下面的 + 开始。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9C97AD),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        } else {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        "无痕窗口（${tabs.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFEDEAFF),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "点击卡片继续浏览",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9C97AD),
                    )
                }
            }
            items(tabs.size) { index ->
                val tab = tabs[index]
                WindowCard(
                    tab = tab,
                    selected = tab.id == activeTabId,
                    onClick = { onSelectTab(tab.id) },
                    onClose = { onCloseTab(tab.id) },
                )
            }
        }
    }
}

/** 两列窗口卡片：头像 + 标题 + 关闭，下方预览区。 */
@Composable
private fun WindowCard(
    tab: BrowserTab,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = AnxinPalette
    val host = Urls.prettyHost(tab.url).ifBlank { "主页" }
    val accent = when {
        tab.isIncognito -> IncognitoAccent
        host.contains("baidu") -> Color(0xFF2B34E8)
        host.contains("mi.com") -> Color(0xFFFF6900)
        host.contains("bilibili") -> Color(0xFFFB7299)
        else -> palette.brand
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) Modifier.border(2.dp, SelectedBorder, RoundedCornerShape(20.dp))
                else Modifier
            )
            .glassPanel(
                radius = 20.dp,
                alpha = if (selected) 0.8f else 0.55f,
                borderAlpha = if (selected) 0f else 0.55f,
                shadowElevation = if (selected) 16.dp else 8.dp,
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (tab.isIncognito) IncognitoAccent.copy(alpha = 0.18f) else anxinField()
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (tab.isIncognito) Icons.Rounded.Lock else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(7.dp))
            Text(
                tab.title.ifBlank { host },
                style = MaterialTheme.typography.labelMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭窗口",
                    tint = palette.inkSecondary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        // 预览区：骨架线条模拟缩略图
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(anxinSurfaceMuted(), accent.copy(alpha = 0.08f))
                    )
                ),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(accent.copy(alpha = 0.16f))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.85f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.ink.copy(alpha = 0.07f))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.ink.copy(alpha = 0.07f))
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.10f))
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SelectedBorder.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "当前",
                        style = MaterialTheme.typography.labelSmall,
                        color = SelectedBorder,
                    )
                }
            }
            if (tab.isLoading) {
                Text(
                    "加载中 ${tab.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyPane(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(84.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(anxinField()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = palette.inkTertiary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = palette.inkTertiary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
