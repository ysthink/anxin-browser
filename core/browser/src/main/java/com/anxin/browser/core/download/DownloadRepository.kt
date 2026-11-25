package com.anxin.browser.core.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 下载中心数据源：底层走系统 DownloadManager，保证前台服务、断点续传、通知由系统托管。
 * 后续接入 Media3 DownloadManager 时，只需替换本类实现，UI 不变。
 */
class DownloadRepository(private val context: Context) {

    private val manager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    fun enqueue(
        url: String,
        mimeType: String? = null,
        contentDisposition: String? = null,
        contentLength: Long = 0L,
        userAgent: String? = null,
        referer: String? = null,
        suggestedName: String? = null,
    ): Long? {
        return runCatching {
            val fileName = suggestedName
                ?: URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("安心浏览器下载")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
                userAgent?.let { addRequestHeader("User-Agent", it) }
                referer?.let { addRequestHeader("Referer", it) }
                CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            }
            val id = manager.enqueue(request)
            _items.value = _items.value + DownloadItem(
                id = id,
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                totalBytes = contentLength,
                status = DownloadStatus.PENDING,
            )
            refresh()
            id
        }.getOrNull()
    }

    fun pause(id: Long) {
        runCatching { manager.remove(id) }
        _items.value = _items.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.PAUSED) else it
        }
    }

    fun remove(id: Long) {
        runCatching { manager.remove(id) }
        _items.value = _items.value.filterNot { it.id == id }
    }

    fun clearFinished() {
        val finished = _items.value.filter {
            it.status == DownloadStatus.SUCCESS || it.status == DownloadStatus.FAILED
        }
        finished.forEach { runCatching { manager.remove(it.id) } }
        _items.value = _items.value.filterNot {
            it.status == DownloadStatus.SUCCESS || it.status == DownloadStatus.FAILED
        }
    }

    /** 轮询系统下载库，刷新进度。由下载页在可见时调用。 */
    fun refresh() {
        runCatching {
            val known = _items.value
            if (known.isEmpty()) return
            val ids = known.map { it.id }.toLongArray()
            val cursor = manager.query(DownloadManager.Query().setFilterById(*ids))
            val updated = mutableListOf<DownloadItem>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val base = known.firstOrNull { it.id == id } ?: continue
                    val statusCode = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    updated += base.copy(
                        downloadedBytes = downloaded,
                        totalBytes = if (total > 0) total else base.totalBytes,
                        status = when (statusCode) {
                            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESS
                            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                            DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                            else -> DownloadStatus.RUNNING
                        },
                        localUri = localUri,
                    )
                }
            }
            if (updated.isNotEmpty()) {
                val byId = updated.associateBy { it.id }
                _items.value = known.map { byId[it.id] ?: it }
            }
        }
    }
}
