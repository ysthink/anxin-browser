package com.anxin.browser.feature.bookmarks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

/**
 * 书签列表页：点击打开，长按删除。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(
    bookmarks: List<Pair<String, String>>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    val sorted = remember(bookmarks) { bookmarks }

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
                PageTitle(title = "书签", count = bookmarks.size)
            }

            if (sorted.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("还没有书签", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                        Text(
                            "在网页菜单中选择「添加书签」",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkTertiary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sorted, key = { it.second }) { (title, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassPanel(radius = 18.dp, alpha = 0.55f, shadowElevation = 8.dp)
                                .combinedClickable(onClick = { onOpen(url) }, onLongClick = { onRemove(url) })
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (Urls.prettyHost(url).take(1).ifBlank { "·" }).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.brandDeep,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    title.ifBlank { url },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    Urls.prettyHost(url).ifBlank { url },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.inkTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "长按删除书签",
                                tint = palette.inkTertiary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable { onRemove(url) },
                            )
                        }
                    }
                }
            }
        }
        AnxinActionBar(
            actions = actions,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
