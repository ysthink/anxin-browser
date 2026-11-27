package com.anxin.browser.feature.downloads

import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.MediaResource
import com.anxin.browser.core.model.DownloadStatus
import com.anxin.browser.design.AnxinEmptyState
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPanel
import com.anxin.browser.design.GlassSegments
import com.anxin.browser.design.SectionHeader
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.ThinProgressBar
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 本地文件（系统下载库 MediaStore.Downloads）。 */
data class LocalFile(
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
)

/**
 * 本地文件列表 = 系统下载库里的文件 + 本应用下载目录里的成品。
 *
 * 后者必须单独补：自研下载器把文件写在应用私有外部目录，
 * 不进 MediaStore，否则会出现"下载完成 1 条、本地文件 0 个"的自相矛盾。
 */
internal fun queryLocalDownloads(context: android.content.Context): List<LocalFile> {
    val files = mutableListOf<LocalFile>()

    val ownDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
    if (ownDir != null && ownDir.isDirectory) {
        ownDir.listFiles { f -> f.isFile && !f.name.endsWith(".part") }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { f ->
                val uri = runCatching {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        f,
                    )
                }.getOrNull()
                if (uri != null) files += LocalFile(uri, f.name, f.length(), f.lastModified())
            }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
            )
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                var added = 0
                while (cursor.moveToNext() && added < 120) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "未命名"
                    if (files.any { it.name == name }) continue
                    files += LocalFile(
                        uri = android.content.ContentUris.withAppendedId(collection, id),
                        name = name,
                        size = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateCol) * 1000L,
                    )
                    added++
                }
            }
        }
    }
    return files
}


/**
 * 已下载文件 -> 可交给其它应用的 Uri。
 *
 * 下载完成后文件会被转存到系统公共 Download 目录，`localUri` 因此是
 * `content://media/external/downloads/...`——直接原样返回，其它应用凭授予的
 * 读权限即可打开，不需要再经 FileProvider。
 *
 * 转存失败（Android 9 及以下）时文件仍在应用私有外部目录，`file://` 在 API 24+
 * 会抛 FileUriExposedException，所以统一经 FileProvider 转换；
 * `localUri` 为空时（例如进程重启后列表已清空）按文件名回落到下载目录。
 */
internal fun resolveDownloadUri(context: android.content.Context, item: DownloadItem?): android.net.Uri? {
    item?.localUri?.takeIf { it.startsWith("content://") }
        ?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
        ?.let { uri -> return uri.takeIf { exists(context, it) } }
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: return null
    val raw = item?.localUri?.takeIf { it.startsWith("file://") }
        ?.let { runCatching { java.io.File(android.net.Uri.parse(it).path!!) }.getOrNull() }
        ?: item?.let { java.io.File(dir, it.fileName) }
    val file = raw ?: return null
    if (!file.exists()) return null
    return runCatching {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
    }.getOrNull()
}

/** content:// Uri 指向的文件是否还在（用户可能在文件管理器里删掉过）。 */
private fun exists(context: android.content.Context, uri: android.net.Uri): Boolean = runCatching {
    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
}.getOrDefault(false)

internal fun openDownloadedFile(context: android.content.Context, item: DownloadItem) {
    val uri = resolveDownloadUri(context, item)
    if (uri == null) {
        Toast.makeText(context, "文件不存在或已被移动", Toast.LENGTH_SHORT).show()
        return
    }
    val type = context.contentResolver.getType(uri) ?: item.mimeType ?: guessMime(item.fileName)
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, type)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        Toast.makeText(context, "没有可打开此文件的应用", Toast.LENGTH_SHORT).show()
    }
}

private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "zip" -> "application/zip"
    "apk" -> "application/vnd.android.package-archive"
    "mp4" -> "video/mp4"
    "mp3" -> "audio/mpeg"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "txt" -> "text/plain"
    else -> "*/*"
}

internal fun openLocalFile(context: android.content.Context, file: LocalFile) {
    runCatching {
        val type = context.contentResolver.getType(file.uri) ?: "*/*"
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(file.uri, type)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        Toast.makeText(context, "没有可打开此文件的应用", Toast.LENGTH_SHORT).show()
    }
}

private data class DemoDownload(
    val name: String,
    val sizeText: String,
    val progress: Float,
    val color: Color,
    val ext: String,
    val subtitle: String? = null,
)

@Composable
fun DownloadsScreen(
    items: List<DownloadItem>,
    media: List<MediaResource>,
    onBack: () -> Unit,
    onOpenFolder: () -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearFinished: () -> Unit,
    onPlayMedia: (MediaResource) -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    val context = LocalContext.current
    var segment by remember { mutableIntStateOf(0) }
    // 以「已完成任务数 + 当前页签」为 key：只用 items.size 不够——下载完成时
    // 任务总数不变，列表不会重扫，会出现"已完成 1 条、本地文件 0 个"的自相矛盾。
    val doneCount = items.count { it.status == DownloadStatus.SUCCESS }
    val localFiles by produceState(
        initialValue = emptyList<LocalFile>(),
        key1 = doneCount,
        key2 = segment,
    ) {
        // 等一拍：落盘的重命名刚发生，立刻扫可能仍只看到 .part。
        kotlinx.coroutines.delay(120)
        value = queryLocalDownloads(context)
    }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val running = items.filter {
        it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PENDING ||
            it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED
    }
    // 失败任务归到"下载中"这一页：它还能点继续重试，摆在"已完成"里既不能点开、
    // 又会被算进"已完成 N 个"的计数，看着像文件已经下好了。
    val finished = items.filter { it.status == DownloadStatus.SUCCESS }

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

            GlassSegments(
                labels = listOf(
                    "下载中 ${running.size}",
                    "已完成 ${finished.size}",
                    "本地文件 ${localFiles.size}",
                    "资源库",
                ),
                selectedIndex = when (segment) {
                    1 -> 1
                    2 -> 3
                    3 -> 2
                    else -> 0
                },
                onSelect = { index ->
                    segment = when (index) {
                        1 -> 1
                        2 -> 3
                        3 -> 2
                        else -> 0
                    }
                },
                paddingStart = 18.dp,
                paddingEnd = 18.dp,
            )

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (segment == 0) {
                    if (running.isEmpty()) {
                        item { EmptyTaskCard(title = "暂无下载任务", hint = "在网页中点击下载后，这里会显示真实任务进度。") }
                    } else {
                        items(running) { item ->
                            DownloadCard(
                                item = item,
                                onToggle = {
                                    when (item.status) {
                                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> onResume(item.id)
                                        else -> onPause(item.id)
                                    }
                                },
                                onDelete = { onDelete(item.id) },
                            )
                        }
                    }
                }

                // "下载完成"分区只在有内容或用户主动切到该页签时出现，
                // 否则"下载中"页签下会挂一个空标题。
                if ((segment == 1 || segment == 0) && (finished.isNotEmpty() || segment == 1)) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        SectionHeader(title = "下载完成")
                    }
                    if (finished.isEmpty()) {
                        item { EmptyTaskCard(title = "暂无已完成任务", hint = "历史任务清空后不再显示。") }
                    } else {
                        items(finished) { item ->
                            FileRow(
                                name = item.fileName,
                                size = formatBytes(item.totalBytes),
                                ext = item.fileName.substringAfterLast('.', "FILE").uppercase().take(4),
                                color = palette.brand,
                                trailing = "打开",
                                onClick = {
                                    if (item.status == DownloadStatus.SUCCESS) {
                                        openDownloadedFile(context, item)
                                    }
                                },
                                onDelete = { onDelete(item.id) },
                            )
                        }
                    }
                    if (finished.isNotEmpty()) item {
                        Text(
                            "共 ${finished.size} 个已完成任务",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkTertiary,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                        )
                    }
                }

                if (segment == 3) {
                    if (localFiles.isEmpty()) {
                        item {
                            GlassPanel(radius = 22.dp, alpha = 0.55f) {
                                Column {
                                    Text("暂无本地文件", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "下载完成后，文件会出现在这里，点击即可用其它应用打开。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.inkSecondary,
                                    )
                                }
                            }
                        }
                    } else {
                        items(localFiles, key = { it.uri.toString() }) { file ->
                            LocalFileRow(
                                file = file,
                                dateText = dateFormat.format(Date(file.dateAdded)),
                                sizeText = formatBytes(file.size),
                                onClick = { openLocalFile(context, file) },
                            )
                        }
                        item {
                            Text(
                                "共 ${localFiles.size} 个本地文件（系统下载库 + 应用下载目录）",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkTertiary,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            )
                        }
                    }
                }

                if (segment == 2) {
                    item {
                        LibrarySection(
                            media = media,
                            localFiles = localFiles,
                            onPlay = onPlayMedia,
                            onOpenFile = { openLocalFile(context, it) },
                        )
                    }
                }

                // 只在任务页签显示；资源库里摆一个"清空下载记录"是错位的。
                if (finished.isNotEmpty() && segment != 2) {
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
        }
        AnxinActionBar(
            actions = actions,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LocalFileRow(
    file: LocalFile,
    dateText: String,
    sizeText: String,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    val ext = file.name.substringAfterLast('.', "").uppercase().take(4)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 18.dp, alpha = 0.5f, shadowElevation = 8.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(ext.ifBlank { "FILE" }, palette.brand, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$dateText · $sizeText",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
            )
        }
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = "打开",
            tint = palette.brand,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DownloadCard(item: DownloadItem, onToggle: () -> Unit, onDelete: () -> Unit) {
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
                        .background(anxinField())
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (item.status) {
                            DownloadStatus.PAUSED, DownloadStatus.FAILED -> Icons.Rounded.PlayArrow
                            DownloadStatus.SUCCESS -> Icons.Rounded.Check
                            else -> Icons.Rounded.Pause
                        },
                        contentDescription = when (item.status) {
                            DownloadStatus.PAUSED, DownloadStatus.FAILED -> "继续"
                            DownloadStatus.SUCCESS -> "已完成"
                            else -> "暂停"
                        },
                        tint = palette.brandDeep,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                // 单条删除：下载中删除会取消任务并清掉 .part，已完成仅移除记录。
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(anxinField())
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "删除记录",
                        tint = palette.inkSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ThinProgressBar(progress = if (item.progress <= 0f) 0.04f else item.progress)
        }
    }
}

@Composable
private fun EmptyTaskCard(title: String, hint: String) {
    AnxinEmptyState(
        icon = Icons.Rounded.Download,
        title = title,
        hint = hint,
    )
}

@Composable
private fun FileRow(
    name: String,
    size: String,
    ext: String,
    color: Color,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 18.dp, alpha = 0.42f, shadowElevation = 6.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileIcon(ext, color, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = palette.ink, maxLines = 1)
            Text(size, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = palette.brand)
        }
        if (onDelete != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.Close,
                contentDescription = "删除记录",
                tint = palette.inkTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
            )
        }
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
