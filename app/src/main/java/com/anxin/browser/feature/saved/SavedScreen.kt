package com.anxin.browser.feature.saved

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.BookmarkItem
import com.anxin.browser.data.HistoryEntry
import com.anxin.browser.design.AnxinEmptyState
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.imeTap
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.glassPanel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书签 + 历史的合体页：顶部两个页签，左右滑动切换，底部一颗关闭按钮。
 *
 * 之前是两个独立页面（各占一条导航），用户想找"上次看过的站点"得先猜它属于
 * 书签还是历史。合成一页后两边的数据仍在各自的分区里，只是切换成本降到一次滑动。
 *
 * 不挂底部悬浮工具栏：这一页是从浏览页临时"掀起来"的浮层，
 * 底部留一颗 X 直接收起，比再多一排导航更符合屏幕上的动作意图。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SavedScreen(
    initialPage: Int = 0,
    bookmarks: List<BookmarkItem>,
    history: List<HistoryEntry>,
    onClose: () -> Unit,
    onOpen: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onAddBookmark: (title: String, url: String, group: String) -> Unit,
    canAddBookmark: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1)) { 2 }
    val dateFormat = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
    val historyGroups = remember(history) {
        history.groupBy { dayFormat.format(Date(it.time)) }
    }
    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    AuroraBackground(modifier = modifier) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ---- 顶部页签：居中，选中项下面一条短线 ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(56.dp),
                ) {
                    SavedTab("书签", pagerState.currentPage == 0) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }
                    SavedTab("历史", pagerState.currentPage == 1) {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
                }

                if (pagerState.currentPage == 0 && canAddBookmark) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.brand)
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "添加书签",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                pageSpacing = 0.dp,
            ) { page ->
                if (page == 0) {
                    BookmarkPage(
                        bookmarks = bookmarks,
                        onOpen = onOpen,
                        onRemove = onRemoveBookmark,
                    )
                } else {
                    HistoryPage(
                        groups = historyGroups,
                        dateFormat = dateFormat,
                        onOpen = onOpen,
                        onRemove = onRemoveHistory,
                        onClear = onClearHistory,
                    )
                }
            }
        }

        // ---- 底部关闭：浮在列表上方，列表滚动时最后一条也能露出来 ----
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .size(46.dp)
                .glassPanel(radius = 23.dp, alpha = 0.66f, borderAlpha = 0.85f, shadowElevation = 14.dp)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "关闭",
                tint = palette.inkSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (showAddDialog) {
        AddBookmarkDialog(
            existingGroups = bookmarks.map { it.group }.distinct(),
            onDismiss = { showAddDialog = false },
            onConfirm = { title, url, group ->
                showAddDialog = false
                onAddBookmark(title, url, group)
            },
        )
    }
}

@Composable
private fun SavedTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AnxinPalette
    val underline by animateFloatAsState(if (selected) 1f else 0f, label = "underline")
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            color = if (selected) palette.ink else palette.inkTertiary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.brand.copy(alpha = underline)),
        )
    }
}

@Composable
private fun BookmarkPage(
    bookmarks: List<BookmarkItem>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val palette = AnxinPalette
    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnxinEmptyState(
                icon = Icons.Rounded.BookmarkBorder,
                title = "还没有书签",
                hint = "点右上角 + 手动添加，或在网页菜单中选择「添加书签」。",
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        return
    }
    // 按分组分区展示；组顺序按书签列表中首次出现的顺序。
    val grouped = remember(bookmarks) { bookmarks.groupBy { it.group } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (group, list) ->
            item(key = "group_$group") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                ) {
                    Text(
                        group,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.inkSecondary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${list.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkTertiary,
                    )
                }
            }
            items(list, key = { it.url }) { bm ->
                SavedRow(
                    title = bm.title.ifBlank { bm.url },
                    subtitle = Urls.prettyHost(bm.url).ifBlank { bm.url },
                    seed = Urls.prettyHost(bm.url).take(1),
                    onClick = { onOpen(bm.url) },
                    onRemove = { onRemove(bm.url) },
                    onLongClick = { onRemove(bm.url) },
                )
            }
        }
    }
}

/** 手动添加书签：标题 / 网址 / 分组（可选已有分组或新建）。 */
@Composable
private fun AddBookmarkDialog(
    existingGroups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, group: String) -> Unit,
) {
    val palette = AnxinPalette
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val defaultGroups = remember(existingGroups) {
        if (existingGroups.contains("默认")) existingGroups else listOf("默认") + existingGroups
    }
    var selectedGroup by remember { mutableStateOf("默认") }
    var creatingGroup by remember { mutableStateOf(false) }
    var newGroup by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .glassPanel(radius = 24.dp, alpha = 0.9f, borderAlpha = 0.9f, shadowElevation = 24.dp)
                // 空点击消费：防止面板内的触摸穿透到底下的遮罩层（会把对话框关掉）。
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {}
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Text(
                "添加书签",
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))

            DialogField(
                value = title,
                onValueChange = { title = it },
                hint = "标题（留空自动取网址）",
            )
            Spacer(Modifier.height(10.dp))
            DialogField(
                value = url,
                onValueChange = { url = it },
                hint = "网址，如 example.com",
            )

            Spacer(Modifier.height(14.dp))
            Text("分组", style = MaterialTheme.typography.labelMedium, color = palette.inkTertiary)
            Spacer(Modifier.height(8.dp))
            if (creatingGroup) {
                DialogField(
                    value = newGroup,
                    onValueChange = { newGroup = it },
                    hint = "新分组名称",
                    autoFocus = true,
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        "取消",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                creatingGroup = false
                                newGroup = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                // 已有分组胶囊 + 新建分组入口
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defaultGroups.take(3).forEach { g ->
                        val selected = selectedGroup == g
                        Text(
                            g,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.brandDeep else palette.inkSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    (if (selected) palette.brand else palette.ink).copy(alpha = 0.10f)
                                )
                                .clickable { selectedGroup = g }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        "＋ 新建",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.brand,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.brand.copy(alpha = 0.08f))
                            .clickable { creatingGroup = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            val targetUrl = url.trim().let {
                if (it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
            }
            val group = if (creatingGroup) newGroup.trim().ifBlank { "默认" } else selectedGroup
            val valid = targetUrl.isNotBlank()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (valid) palette.brand else palette.ink.copy(alpha = 0.12f)
                    )
                    .clickable(enabled = valid) {
                        onConfirm(title.trim(), targetUrl, group)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "保存到书签",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (valid) Color.White else palette.inkTertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    autoFocus: Boolean = false,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(anxinField())
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isBlank()) {
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = palette.inkTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
                cursorBrush = SolidColor(palette.brand),
                modifier = Modifier.imeTap().fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HistoryPage(
    groups: Map<String, List<HistoryEntry>>,
    dateFormat: SimpleDateFormat,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    val palette = AnxinPalette
    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnxinEmptyState(
                icon = Icons.Rounded.History,
                title = "暂无历史记录",
                hint = "浏览过的网页会按时间归档在这里，无痕访问不会留下记录。",
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "clear_all") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "无痕浏览产生的记录不会出现在这里",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = palette.inkTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("清空", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
                }
            }
        }
        groups.forEach { (day, list) ->
            item(key = "day_$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.inkSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
            items(list, key = { it.url + it.time }) { entry ->
                SavedRow(
                    title = entry.title.ifBlank { entry.url },
                    subtitle = "${dateFormat.format(Date(entry.time))} · ${Urls.prettyHost(entry.url)}",
                    seed = Urls.prettyHost(entry.url).take(1),
                    onClick = { onOpen(entry.url) },
                    onRemove = { onRemove(entry.url) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SavedRow(
    title: String,
    subtitle: String,
    seed: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 18.dp, alpha = 0.55f, shadowElevation = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(anxinField()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                seed.ifBlank { "·" }.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = palette.brandDeep,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "删除",
                tint = palette.inkTertiary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}
