package com.anxin.browser.feature.downloads

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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.DownloadStatus
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPanel
import com.anxin.browser.design.GlassSegment
import com.anxin.browser.design.SectionHeader
import com.anxin.browser.design.ThinProgressBar
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

private data class DemoDownload(
    val name: String,
    val sizeText: String,
    val progress: Float,
    val color: Color,
    val ext: String,
    val subtitle: String? = null,
)

private val demoRunning = listOf(
    DemoDownload("大自然的奇迹_1080P.mp4", "68.5MB / 745.6MB", 0.09f, Color(0xFF4A6BFF), "MP4", "2.3MB/s"),
    DemoDownload("文档资料.zip", "12.4MB / 50.0MB", 0.24f, Color(0xFFFF9F43), "ZIP", "1.1MB/s"),
)

private val demoFinished = listOf(
    DemoDownload("壁纸合集.jpg", "3.2MB", 1f, Color(0xFF23C08A), "JPG"),
    DemoDownload("旅行攻略.pdf", "1.8MB", 1f, Color(0xFFE1251B), "PDF"),
    DemoDownload("离线音乐.m4a", "8.6MB", 1f, Color(0xFF7B5CFF), "M4A"),
)

@Composable
fun DownloadsScreen(
    items: List<DownloadItem>,
    onBack: () -> Unit,
    onOpenFolder: () -> Unit,
    onPause: (Long) -> Unit,
    onClearFinished: () -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    var segment by remember { mutableIntStateOf(0) }
    val running = items.filter {
        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PENDING ||
            it.status == DownloadStatus.PAUSED
    }
    val finished = items.filter {
        it.status == DownloadStatus.SUCCESS || it.status == DownloadStatus.FAILED
    }
    val usingDemo = items.isEmpty()

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
                PageTitle(title = "下载管理")
                Spacer(Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.FolderOpen, "下载目录", size = 36.dp, onClick = onOpenFolder)
            }

            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassSegment("下载中 (${if (usingDemo) demoRunning.size else running.size})", segment == 0) { segment = 0 }
                GlassSegment("已完成 (${if (usingDemo) demoFinished.size else finished.size})", segment == 1) { segment = 1 }
                GlassSegment("资源库", segment == 2) { segment = 2 }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (segment == 0) {
                    if (usingDemo) {
                        items(demoRunning) { DownloadCardDemo(it, onPause = { }) }
                    } else {
                        items(running) { item ->
                            DownloadCard(
                                item = item,
                                onPause = { onPause(item.id) },
                            )
                        }
                    }
                }

                if (segment == 1 || segment == 0) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionHeader(
                            title = "下载完成",
                            action = "更多",
                        )
                    }
                    if (usingDemo) {
                        items(demoFinished) { FileRow(it.name, it.sizeText, it.ext, it.color) }
                    } else {
                        items(finished) { item ->
                            FileRow(
                                name = item.fileName,
                                size = formatBytes(item.totalBytes),
                                ext = item.fileName.substringAfterLast('.', "FILE").uppercase().take(4),
                                color = palette.brand,
                            )
                        }
                    }
                    item {
                        Text(
                            if (usingDemo) "当前为演示数据：在网页中点击下载后，这里会显示真实任务进度。"
                            else "共 ${finished.size} 个已完成任务",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkTertiary,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                        )
                    }
                }

                if (segment == 2) {
                    item {
                        GlassPanel(radius = 22.dp, alpha = 0.55f) {
                            Column {
                                Text("资源库", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "按视频、音频、图片、文档自动归类，媒体嗅探到的资源会汇总到这里。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.inkSecondary,
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .glassPanel(radius = 25.dp, alpha = 0.45f, shadowElevation = 10.dp)
                            .clickable { onClearFinished() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            tint = palette.inkSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("清空下载记录", style = MaterialTheme.typography.titleSmall, color = palette.ink)
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
private fun DownloadCard(item: DownloadItem, onPause: () -> Unit) {
    val palette = AnxinPalette
    val ext = item.fileName.substringAfterLast('.', "FILE").uppercase().take(4)
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        radius = 22.dp,
        alpha = 0.58f,
        shadowElevation = 14.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileIcon(ext, palette.brand)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}   ${item.speedText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable(onClick = onPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (item.status == DownloadStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = palette.brandDeep,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ThinProgressBar(progress = if (item.progress <= 0f) 0.04f else item.progress)
        }
    }
}

@Composable
private fun DownloadCardDemo(item: DemoDownload, onPause: () -> Unit) {
    val palette = AnxinPalette
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        radius = 22.dp,
        alpha = 0.58f,
        shadowElevation = 14.dp,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FileIcon(item.ext, item.color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${item.sizeText}   ${item.subtitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable(onClick = onPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = palette.brandDeep,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ThinProgressBar(progress = item.progress)
        }
    }
}

@Composable
private fun FileRow(name: String, size: String, ext: String, color: Color) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 18.dp, alpha = 0.42f, shadowElevation = 6.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(ext, color, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1)
            Text(size, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
        }
        Icon(
            Icons.Rounded.MoreHoriz,
            contentDescription = null,
            tint = palette.inkTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FileIcon(ext: String, color: Color, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        if (ext.isBlank()) {
            Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
        } else {
            Text(ext, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "--"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1fGB", gb)
        mb >= 1 -> String.format("%.1fMB", mb)
        else -> String.format("%.0fKB", kb)
    }
}
