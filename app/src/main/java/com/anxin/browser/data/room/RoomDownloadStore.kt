package com.anxin.browser.data.room

import com.anxin.browser.core.download.DownloadRecord
import com.anxin.browser.core.download.DownloadStore
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.DownloadStatus

/**
 * [DownloadStore] 的 Room 实现。
 *
 * 状态以字符串存库：`DownloadStatus` 在 core 模块，Entity 在 app 模块，
 * 用枚举需要在两边都引入类型转换器，收益不大；字符串还能容忍枚举增删。
 */
class RoomDownloadStore(private val dao: DownloadDao) : DownloadStore {

    override suspend fun load(): List<DownloadRecord> =
        dao.loadAll().map { it.toRecord() }

    override suspend fun upsert(record: DownloadRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearFinished() = dao.clearFinished()

    private fun DownloadEntity.toRecord(): DownloadRecord = DownloadRecord(
        item = DownloadItem(
            id = id,
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            totalBytes = totalBytes,
            downloadedBytes = downloadedBytes,
            status = runCatching { DownloadStatus.valueOf(status) }
                .getOrDefault(DownloadStatus.FAILED),
            localUri = localUri,
            createdAt = createdAt,
            fromIncognito = fromIncognito,
        ),
        userAgent = userAgent,
        cookie = cookie,
        referer = referer,
    )

    private fun DownloadRecord.toEntity(): DownloadEntity = DownloadEntity(
        id = item.id,
        fileName = item.fileName,
        url = item.url,
        mimeType = item.mimeType,
        totalBytes = item.totalBytes,
        downloadedBytes = item.downloadedBytes,
        status = item.status.name,
        localUri = item.localUri,
        createdAt = item.createdAt,
        fromIncognito = item.fromIncognito,
        userAgent = userAgent,
        cookie = cookie,
        referer = referer,
    )
}
