package com.anxin.browser.core.download

import com.anxin.browser.core.model.DownloadItem

/**
 * 一条可持久化的下载记录。
 *
 * 除了 [DownloadItem] 本身，还要带上发起请求时的 UA / Cookie / Referer——
 * 断点续传是重新发一次 HTTP 请求，服务端（尤其带防盗链的图床、CDN）会校验这些头。
 * 少了它们，进程重启后"继续"会直接 403。
 */
data class DownloadRecord(
    val item: DownloadItem,
    val userAgent: String? = null,
    val cookie: String? = null,
    val referer: String? = null,
)

/**
 * 下载任务的持久化出口。
 *
 * 放在 core 层是因为 [DownloadRepository] 在这里，而 Room 数据库在 app 层，
 * 依赖方向不允许反向引用。app 侧用 Room 实现，测试或未接入时用 [NoDownloadStore]。
 */
interface DownloadStore {
    /** 读取全部历史任务（含已完成的），按创建时间升序。 */
    suspend fun load(): List<DownloadRecord>

    /** 新增或整体覆盖一条记录。 */
    suspend fun upsert(record: DownloadRecord)

    suspend fun delete(id: Long)

    /** 删除所有已结束（成功/失败）的记录。 */
    suspend fun clearFinished()
}

/** 不落库的空实现：进程退出即丢失任务列表，用于测试或未接入 Room 的场景。 */
object NoDownloadStore : DownloadStore {
    override suspend fun load(): List<DownloadRecord> = emptyList()
    override suspend fun upsert(record: DownloadRecord) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun clearFinished() = Unit
}
