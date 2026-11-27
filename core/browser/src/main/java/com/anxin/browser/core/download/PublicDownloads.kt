package com.anxin.browser.core.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File

/**
 * 把下载完成的文件转存到系统公共「Download」目录。
 *
 * 为什么要转存：断点续传需要对临时文件随机写入，`.part` 只能放在应用私有外部目录
 * （`Android/data/<包名>/files/Download`）。那里的成品用户用系统「文件管理」看不到，
 * 换台设备或卸载重装就彻底找不回——下载完的东西等于"消失"。
 *
 * Android 10 (Q) 起通过 MediaStore 往公共目录插入文件**不需要任何存储权限**；
 * Android 9 及以下写公共目录必须申请 WRITE_EXTERNAL_STORAGE，为了不引入权限弹窗，
 * 这些老系统继续把文件留在私有目录（下载页仍能打开，只是不出现在系统文件管理器里）。
 */
object PublicDownloads {

    data class Published(val uri: Uri, val displayName: String)

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * 复制 [source] 到公共 Download 目录。
     *
     * 失败（无权限、磁盘满、厂商魔改 MediaStore）返回 null，调用方保留私有副本即可，
     * 绝不能因为转存失败而把已经下好的文件丢掉。
     */
    fun publish(context: Context, source: File): Published? {
        if (!isSupported) return null
        if (!source.isFile || source.length() <= 0L) return null
        return runCatching { publishQ(context, source) }.getOrNull()
    }

    /** 公共目录里是否已有同名文件（决定落盘名要不要加 (1)）。 */
    fun exists(context: Context, displayName: String): Boolean {
        if (!isSupported) return false
        return runCatching {
            context.contentResolver.query(
                collection(),
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(displayName),
                null,
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishQ(context: Context, source: File): Published? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeOf(source.name))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // 写完再"发布"：否则复制过程中文件管理器就能看到一个大小不对的残缺文件。
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection(), values) ?: return null

        val copied = runCatching {
            resolver.openOutputStream(uri, "w")?.use { out ->
                source.inputStream().use { input -> input.copyTo(out, 256 * 1024) }
            } ?: error("openOutputStream returned null")
        }.isSuccess
        if (!copied) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }

        runCatching {
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
        }
        return Published(uri, displayName(resolver, uri) ?: source.name)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collection(): Uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun displayName(resolver: android.content.ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull()

    /** MediaStore 需要 MIME 才能把文件归到正确的类型，猜不准时退回 octet-stream。 */
    private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4" -> "video/mp4"
        "m4v" -> "video/x-m4v"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "apk" -> "application/vnd.android.package-archive"
        "txt" -> "text/plain"
        "html", "htm" -> "text/html"
        "json" -> "application/json"
        "epub" -> "application/epub+zip"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> "application/octet-stream"
    }
}
