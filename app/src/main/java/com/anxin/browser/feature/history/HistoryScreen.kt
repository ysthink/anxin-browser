package com.anxin.browser.feature.history

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.HistoryEntry
import com.anxin.browser.design.AnxinEmptyState
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录页：按日期分组展示，点击打开，可单条删除或一键清空。
 */
@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    val format = remember { SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault()) }
    val groups = remember(entries) {
        entries.groupBy { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(Date(it.time)) }
    }

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
                PageTitle(title = "历史", count = entries.size)
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.DeleteSweep, "清空历史", size = 36.dp, onClick = onClear)
            }

            Text(
                "无痕浏览产生的记录不会出现在这里",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )

            if (entries.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnxinEmptyState(
                        icon = Icons.Rounded.History,
                        title = "暂无历史记录",
                        hint = "浏览过的网页会按时间归档在这里，无痕访问不会留下记录。",
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassPanel(radius = 18.dp, alpha = 0.55f, shadowElevation = 8.dp)
                                    .clickable { onOpen(entry.url) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(anxinField()),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        (Urls.prettyHost(entry.url).take(1).ifBlank { "·" }).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.brandDeep,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        entry.title.ifBlank { entry.url },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${format.format(Date(entry.time))} · ${Urls.prettyHost(entry.url)}",
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
                                        .clickable { onRemove(entry.url) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "删除该记录",
                                        tint = palette.inkTertiary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
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
