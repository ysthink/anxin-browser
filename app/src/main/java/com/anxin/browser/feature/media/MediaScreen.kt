package com.anxin.browser.feature.media

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.MediaType
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinEmptyState
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassSegments
import com.anxin.browser.design.IconBubble
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

/**
 * 我的视频 / 资源嗅探：展示页面层嗅探到的媒体资源。
 * 分组：全部 / 视频（含 HLS/DASH）/ 音频 / 图片。
 */
@Composable
fun MediaScreen(
    resources: List<MediaResource>,
    onBack: () -> Unit,
    onPlay: (MediaResource) -> Unit,
    onClear: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    val context = LocalContext.current
    var segment by remember { mutableIntStateOf(0) }

    val shown = remember(resources, segment) {
        when (segment) {
            1 -> resources.filter {
                it.type in listOf(MediaType.VIDEO, MediaType.HLS, MediaType.DASH)
            }
            2 -> resources.filter { it.type == MediaType.AUDIO }
            3 -> resources.filter { it.type == MediaType.IMAGE }
            else -> resources
        }.reversed()
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
                PageTitle(title = "我的视频", subtitle = "页面层嗅探 · 仅本页可见")
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.DeleteSweep, "清空", size = 36.dp, onClick = onClear)
            }

            GlassSegments(
                labels = listOf(
                    "全部 (${resources.size})",
                    "视频",
                    "音频",
                    "图片",
                ),
                selectedIndex = segment,
                onSelect = { segment = it },
                modifier = Modifier,
                paddingStart = 18.dp,
                paddingEnd = 18.dp,
            )

            Spacer(Modifier.height(12.dp))

            if (shown.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AnxinEmptyState(
                        icon = Icons.Rounded.VideoLibrary,
                        title = "暂未嗅探到媒体资源",
                        hint = "打开带视频的网页后回到这里查看，资源仅在当前会话中保留。",
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    // 底部留出悬浮工具栏的高度，否则最后一条会被胶囊压住。
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.url }) { res ->
                        MediaRow(
                            res = res,
                            onPlay = { onPlay(res) },
                            onCopy = {
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("url", res.url))
                            },
                        )
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

@Composable
private fun MediaRow(
    res: MediaResource,
    onPlay: () -> Unit,
    onCopy: () -> Unit,
) {
    val palette = AnxinPalette
    val (icon, typeText, tint) = when (res.type) {
        MediaType.HLS -> Triple(Icons.Rounded.OndemandVideo, "HLS", palette.violet)
        MediaType.DASH -> Triple(Icons.Rounded.OndemandVideo, "DASH", palette.violet)
        MediaType.VIDEO -> Triple(Icons.Rounded.PlayCircle, "视频", palette.brand)
        MediaType.AUDIO -> Triple(Icons.Rounded.PlayCircle, "音频", palette.cyan)
        MediaType.IMAGE -> Triple(Icons.Rounded.Photo, "图片", palette.warning)
        else -> Triple(Icons.Rounded.Link, "文件", palette.inkSecondary)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 18.dp, alpha = 0.55f, shadowElevation = 8.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, tint, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                res.title ?: Urls.prettyHost(res.url),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$typeText · ${res.url.take(46)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onCopy),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "复制链接",
                tint = palette.inkSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        if (res.type != MediaType.IMAGE && res.type != MediaType.FILE) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayCircle,
                    contentDescription = "播放",
                    tint = palette.brand,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
