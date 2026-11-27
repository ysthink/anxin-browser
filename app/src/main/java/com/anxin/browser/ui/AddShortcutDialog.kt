package com.anxin.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.HistoryEntry
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.GlassPrimaryButton
import com.anxin.browser.design.GlassSegments
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.imeTap
import com.anxin.browser.design.glassPanel

/**
 * 快捷入口添加面板：书签列表 / 新建书签 / 历史记录 三个页签。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShortcutDialog(
    bookmarks: List<Pair<String, String>>,
    history: List<HistoryEntry>,
    onDismiss: () -> Unit,
    onSelectBookmark: (title: String, url: String) -> Unit,
    onSelectHistory: (title: String, url: String) -> Unit,
    onNewBookmark: (label: String, url: String) -> Unit,
) {
    val palette = AnxinPalette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.24f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 14.dp)
                .glassPanel(radius = 28.dp, alpha = 0.92f, borderAlpha = 0.95f, shadowElevation = 24.dp)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text("添加快捷入口", style = MaterialTheme.typography.titleLarge, color = palette.ink)
            Spacer(Modifier.height(4.dp))
            Text("从书签或历史选择，或直接新建", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)

            Spacer(Modifier.height(14.dp))
            GlassSegments(
                labels = listOf("书签 (${bookmarks.size})", "历史", "新建书签"),
                selectedIndex = tab,
                onSelect = { tab = it },
                paddingStart = 0.dp,
                paddingEnd = 0.dp,
            )

            Spacer(Modifier.height(14.dp))

            when (tab) {
                0 -> EntryList(
                    empty = bookmarks.isEmpty(),
                    emptyText = "暂无书签，可在网页菜单中添加",
                ) {
                    items(bookmarks, key = { it.second }) { (title, url) ->
                        EntryRow(
                            title = title,
                            subtitle = Urls.prettyHost(url),
                            icon = Icons.Rounded.Bookmark,
                            onClick = {
                                onSelectBookmark(title, url)
                                onDismiss()
                            },
                        )
                    }
                }

                1 -> EntryList(
                    empty = history.isEmpty(),
                    emptyText = "暂无历史记录",
                ) {
                    items(history, key = { it.url }) { entry ->
                        EntryRow(
                            title = entry.title.ifBlank { Urls.prettyHost(entry.url) },
                            subtitle = Urls.prettyHost(entry.url),
                            icon = Icons.Rounded.History,
                            onClick = {
                                onSelectHistory(entry.title, entry.url)
                                onDismiss()
                            },
                        )
                    }
                }

                else -> NewBookmarkForm(onNewBookmark = {
                    onNewBookmark(it.first, it.second)
                    onDismiss()
                })
            }
        }
    }
}

@Composable
private fun EntryList(
    empty: Boolean,
    emptyText: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    if (empty) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = AnxinPalette.inkTertiary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun EntryRow(
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
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(anxinField()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.brand, modifier = Modifier.size(17.dp))
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
                subtitle.ifBlank { "about:blank" },
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NewBookmarkForm(onNewBookmark: (Pair<String, String>) -> Unit) {
    val palette = AnxinPalette
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column {
        LabeledField("名称", label, onLabelChange = { label = it })
        Spacer(Modifier.height(10.dp))
        LabeledField("网址（如 news.cn 或 https://news.cn）", url, onLabelChange = { url = it })
        Spacer(Modifier.height(18.dp))
        GlassPrimaryButton(
            text = "添加",
            icon = Icons.Rounded.Add,
            enabled = label.isNotBlank() && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNewBookmark(label.trim() to url.trim()) },
        )
    }
}

@Composable
private fun LabeledField(
    hint: String,
    value: String,
    onLabelChange: (String) -> Unit,
) {
    val palette = AnxinPalette
    Column {
        Text(hint, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(anxinField())
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Public,
                    contentDescription = null,
                    tint = palette.inkTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onLabelChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = palette.ink,
                            fontWeight = FontWeight.Normal,
                        ),
                        cursorBrush = SolidColor(palette.brand),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions.Default,
                        modifier = Modifier.imeTap().fillMaxWidth(),
                    )
                    if (value.isEmpty()) {
                        Text(
                            when (hint.startsWith("名称")) { true -> "例如 深度求索" else -> "例如 https://chat.deepseek.com" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.inkTertiary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = palette.inkTertiary.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("仅保存在本机", style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
        }
    }
}
