package com.anxin.browser.feature.newtab

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.HistoryEntry
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.imeTap
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPanel
import com.anxin.browser.design.IconBubble
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions

/**
 * 搜索体验页（新标签页）：
 * - 搜索引擎切换栏 + 搜索框；
 * - 输入辅助：www. / https:// / 剪贴板 / 无痕浏览；
 * - 聚合区：搜索历史（可清空）、我的书签（可隐藏）、经常访问（可隐藏）。
 */
@Composable
fun NewTabScreen(
    actions: NavActions,
    tabCount: Int,
    onClose: () -> Unit,
    onAddShortcut: () -> Unit,
    searchEngine: String,
    showEngineBar: Boolean = true,
    onPickEngine: () -> Unit,
    searches: List<String>,
    onClearSearches: () -> Unit,
    bookmarks: List<Pair<String, String>>,
    frequent: List<HistoryEntry>,
    onSubmit: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenIncognito: () -> Unit,
) {
    val palette = AnxinPalette
    var query by rememberSaveable { mutableStateOf("") }
    var showBookmarks by rememberSaveable { mutableStateOf(true) }
    var showFrequent by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current

    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "搜索",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Close, "关闭", size = 36.dp, onClick = onClose)
            }

            // ---- 搜索引擎切换栏 ----
            if (showEngineBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "搜索引擎",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkTertiary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                        .background(anxinField())
                        .clickable(onClick = onPickEngine)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$searchEngine · 点击切换",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.brandDeep,
                        )
                    }
                }
            }

            // ---- 搜索框 ----
            SearchField(
                query = query,
                onQueryChange = { query = it },
                onSubmit = {
                    if (query.isNotBlank()) {
                        onSubmit(query)
                        query = ""
                    }
                },
            )

            // ---- 输入辅助 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip("www.") { query = "www." + query.removePrefix("https://").removePrefix("http://") }
                AssistChip("https://") { query = "https://" + query.removePrefix("https://").removePrefix("http://").removePrefix("www.") }
                AssistChip("剪贴板", icon = Icons.Rounded.ContentPaste) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = runCatching { cm.primaryClip?.getItemAt(0)?.text?.toString() }.getOrNull()
                    if (!text.isNullOrBlank()) query = text
                }
                AssistChip("无痕浏览", icon = Icons.Rounded.VisibilityOff, onClick = onOpenIncognito)
            }

            // ---- 聚合区 ----
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 搜索历史
                item {
                    SectionHeader("搜索历史", "清空", enabled = searches.isNotEmpty(), onAction = onClearSearches)
                }
                if (searches.isEmpty()) {
                    item { EmptyHint("暂无搜索历史") }
                } else {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            searches.take(4).forEach { term ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(anxinField())
                                        .clickable { onSubmit(term) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        term,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.inkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 76.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // 我的书签
                item {
                    Spacer(Modifier.height(6.dp))
                    SectionHeader(
                        "我的书签",
                        if (showBookmarks) "隐藏" else "显示",
                        enabled = true,
                        onAction = { showBookmarks = !showBookmarks },
                    )
                }
                if (showBookmarks) {
                    if (bookmarks.isEmpty()) {
                        item {
                            EmptyHint("暂无书签，去网页菜单「添加书签」") { onAddShortcut() }
                        }
                    } else {
                        items(bookmarks.take(6), key = { "bm_${it.second}" }) { (title, url) ->
                            ListRow(title.ifBlank { url }, Urls.prettyHost(url), Icons.Rounded.Bookmark) {
                                onOpenUrl(url)
                            }
                        }
                        if (bookmarks.size > 6) {
                            item {
                                Text(
                                    "共 ${bookmarks.size} 条书签",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.inkTertiary,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }

                // 经常访问
                item {
                    Spacer(Modifier.height(6.dp))
                    SectionHeader(
                        "经常访问",
                        if (showFrequent) "隐藏" else "显示",
                        enabled = true,
                        onAction = { showFrequent = !showFrequent },
                    )
                }
                if (showFrequent) {
                    if (frequent.isEmpty()) {
                        item { EmptyHint("访问的网站会出现在这里") }
                    } else {
                        items(frequent, key = { "fq_${it.url}" }) { entry ->
                            ListRow(
                                entry.title.ifBlank { Urls.prettyHost(entry.url) },
                                Urls.prettyHost(entry.url),
                                Icons.Rounded.Public,
                            ) { onOpenUrl(entry.url) }
                        }
                    }
                }
            }
        }
        AnxinActionBar(
            actions = actions,
            tabCount = tabCount,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .height(50.dp)
            .glassPanel(radius = 25.dp, alpha = 0.85f, borderAlpha = 0.95f, shadowElevation = 14.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
            cursorBrush = SolidColor(palette.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.imeTap().weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("搜索或输入网址", style = MaterialTheme.typography.bodyMedium, color = palette.inkTertiary)
                }
                inner()
            },
        )
        Icon(
            Icons.Rounded.ArrowForward,
            contentDescription = "前往",
            tint = if (query.isNotBlank()) palette.brand else palette.inkTertiary.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .clickable(enabled = query.isNotBlank(), onClick = onSubmit),
        )
    }
}

@Composable
private fun AssistChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(anxinField())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = palette.brand, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.inkSecondary)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        Spacer(Modifier.weight(1f))
        Text(
            action,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) palette.brand else palette.inkTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(enabled = enabled, onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyHint(text: String, onClick: (() -> Unit)? = null) {
    val palette = AnxinPalette
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = palette.inkSecondary,
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
    )
}

@Composable
private fun ListRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(anxinField())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, palette.brand, size = 34.dp)
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
    }
}
