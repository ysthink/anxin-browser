package com.anxin.browser.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.MediaType
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.glassPanel

/**
 * 资源库：把"网页里嗅探到的媒体"和"已经落到本机的文件"放在同一个视图里。
 *
 * 两者是同一件事的两个阶段——嗅探到的资源是"可下载"，本地文件是"已下载"，
 * 分开放会让用户在两处找同一个文件。按媒体类型归类，让"我要那个视频"这个念头
 * 一次就能被满足，而不用回忆它当时是在哪一页被嗅探到的。
 */
@Composable
internal fun LibrarySection(
    media: List<MediaResource>,
    localFiles: List<LocalFile>,
    onPlay: (MediaResource) -> Unit,
    onOpenFile: (LocalFile) -> Unit,
) {
    val palette = AnxinPalette

    val buckets = listOf(
        Bucket.Movie,
        Bucket.Audio,
        Bucket.Picture,
        Bucket.Doc,
    )
    val counts = buckets.associateWith { bucket ->
        media.count { bucket.matches(it.type) } + localFiles.count { bucket.matches(it.name) }
    }

    Column {
        // 概览：一眼看清库里有什么，而不必滚一遍
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 22.dp, alpha = 0.55f)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            buckets.forEach { bucket ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${counts[bucket] ?: 0}",
                        style = MaterialTheme.typography.titleLarge,
                        color = bucket.tint(palette),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkTertiary,
                    )
                }
            }
        }

        if (media.isEmpty() && localFiles.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(radius = 22.dp, alpha = 0.45f)
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "还没有可归类的资源",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "浏览含视频或图片的网页时，资源会自动出现在这里；下载完成的文件也会归档。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkTertiary,
                )
            }
            return@Column
        }

        // 网页资源（嗅探得到，尚未下载）
        val webMedia = media.reversed()
        if (webMedia.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LibraryHeader(title = "网页资源", trailing = "${webMedia.size} 项")
            webMedia.take(40).forEach { res ->
                ResourceRow(
                    icon = bucketOf(res.type).icon,
                    tint = bucketOf(res.type).tint(palette),
                    title = res.title?.takeIf { it.isNotBlank() }
                        ?: res.url.substringAfterLast('/').take(48).ifBlank { "媒体资源" },
                    subtitle = Urls.prettyHost(res.referer ?: res.url),
                    tag = bucketOf(res.type).label,
                    actionText = if (res.type == MediaType.IMAGE) "查看" else "播放",
                    onClick = { onPlay(res) },
                )
            }
            if (webMedia.size > 40) {
                Text(
                    "仅显示最近 40 项，共 ${webMedia.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }

        // 本地文件（已落盘）
        if (localFiles.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LibraryHeader(title = "本地文件", trailing = "${localFiles.size} 个")
            localFiles.take(60).forEach { file ->
                val bucket = bucketOf(file.name)
                ResourceRow(
                    icon = bucket.icon,
                    tint = bucket.tint(palette),
                    title = file.name,
                    subtitle = formatBytes(file.size),
                    tag = bucket.label,
                    actionText = "打开",
                    onClick = { onOpenFile(file) },
                )
            }
            if (localFiles.size > 60) {
                Text(
                    "仅显示前 60 个，共 ${localFiles.size} 个",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ResourceRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    tag: String,
    actionText: String,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .glassPanel(radius = 18.dp, alpha = 0.42f, shadowElevation = 6.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(tint.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$tag · $subtitle",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            actionText,
            style = MaterialTheme.typography.labelMedium,
            color = palette.brand,
        )
    }
}


@Composable
private fun LibraryHeader(title: String, trailing: String) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
        Spacer(Modifier.width(8.dp))
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
    }
}

/** 与下载页保持一致的体积格式；本地文件行的副标题要用。 */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format("%.1f GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format("%.1f MB", bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> String.format("%.0f KB", bytes / (1L shl 10).toDouble())
    else -> "$bytes B"
}

/** 归类：网页媒体按嗅探类型，本地文件按扩展名。 */
private enum class Bucket(
    val label: String,
    val icon: ImageVector,
    val extensions: Set<String>,
) {
    Movie("视频", Icons.Rounded.Movie, setOf("mp4", "mkv", "webm", "mov", "avi", "m3u8", "ts", "flv")),
    Audio("音频", Icons.Rounded.MusicNote, setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "opus")),
    Picture("图片", Icons.Rounded.Image, setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic")),
    Doc("文档", Icons.Rounded.Description, setOf("pdf", "txt", "md", "zip", "apk", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "gz", "tar", "rar", "7z")),
    ;

    fun matches(type: MediaType): Boolean = when (this) {
        Movie -> type == MediaType.VIDEO || type == MediaType.HLS || type == MediaType.DASH
        Audio -> type == MediaType.AUDIO
        Picture -> type == MediaType.IMAGE
        Doc -> type == MediaType.FILE
    }

    fun matches(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return this == Doc
        // 未收录的扩展名一律归到"文档"，总比从列表里消失好。
        return if (this == Doc) {
            ext !in Movie.extensions && ext !in Audio.extensions && ext !in Picture.extensions
        } else {
            ext in extensions
        }
    }

    fun tint(palette: com.anxin.browser.design.AnxinColors): Color = when (this) {
        Movie -> palette.brand
        Audio -> Color(0xFF8B5CF6)
        Picture -> Color(0xFF0EA5A5)
        Doc -> Color(0xFFE08A2E)
    }
}

private fun bucketOf(type: MediaType): Bucket = when (type) {
    MediaType.VIDEO, MediaType.HLS, MediaType.DASH -> Bucket.Movie
    MediaType.AUDIO -> Bucket.Audio
    MediaType.IMAGE -> Bucket.Picture
    // 网页里嗅探到的普通文件（pdf/zip 等）归到文档。
    MediaType.FILE -> Bucket.Doc
}

private fun bucketOf(fileName: String): Bucket = when {
    Bucket.Movie.matches(fileName) -> Bucket.Movie
    Bucket.Audio.matches(fileName) -> Bucket.Audio
    Bucket.Picture.matches(fileName) -> Bucket.Picture
    else -> Bucket.Doc
}
