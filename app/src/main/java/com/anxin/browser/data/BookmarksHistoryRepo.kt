package com.anxin.browser.data

import androidx.room.Room
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.room.AnxinDatabase
import com.anxin.browser.data.room.BookmarkEntity
import com.anxin.browser.data.room.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 书签 + 浏览历史的 Room 仓库。
 * - 首次启动从旧 DataStore（JSON/文本行）一次性导入，之后以 Room 为准；
 * - record 由内核访问回调触发（非协程上下文），内部自带 IO 协程；
 * - 搜索历史仍留在 HistoryStore（量小、变更频繁）。
 */
class BookmarksHistoryRepo(context: android.content.Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AnxinDatabase::class.java,
        "anxin_data.db",
    ).fallbackToDestructiveMigration().build()

    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 书签（新在前）。 */
    val bookmarks: Flow<List<Pair<String, String>>> = bookmarkDao.observeAll().map { list ->
        list.map { it.title to it.url }
    }

    /** 浏览历史（最新在前，最多 300 条）。 */
    val history: Flow<List<HistoryEntry>> = historyDao.observeRecent().map { list ->
        list.map { HistoryEntry(it.title, it.url, it.time) }
    }

    /** 经常访问：按域名聚合，取访问次数最多的前 [limit] 个。 */
    fun frequentlyVisited(limit: Int = 4): Flow<List<HistoryEntry>> = history.map { current ->
        current
            .filter { it.url.startsWith("http://") || it.url.startsWith("https://") }
            .groupBy { Urls.hostOf(it.url) }
            .filterKeys { it.isNotBlank() }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<HistoryEntry>>> { it.value.size }
                    .thenByDescending { it.value.maxOf { e -> e.time } },
            )
            .take(limit)
            .map { (_, visits) ->
                val latest = visits.maxBy { it.time }
                HistoryEntry(Urls.prettyHost(latest.url), latest.url, latest.time)
            }
    }

    fun record(title: String, url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        scope.launch {
            val existing = historyDao.findByUrl(url)
            if (existing != null) {
                historyDao.update(
                    existing.copy(
                        title = (title.ifBlank { existing.title }).take(120),
                        time = System.currentTimeMillis(),
                    ),
                )
            } else {
                historyDao.insert(
                    HistoryEntity(title = title.take(120), url = url, time = System.currentTimeMillis()),
                )
            }
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insert(BookmarkEntity(title = (title.ifBlank { url }).take(120), url = url))
    }

    suspend fun removeBookmark(url: String) = bookmarkDao.deleteByUrl(url)

    suspend fun removeHistory(url: String) = historyDao.deleteByUrl(url)

    suspend fun clearHistory() = historyDao.clear()

    suspend fun clearBookmarks() = bookmarkDao.clear()

    /**
     * 一次性迁移：旧 SettingsStore 书签 + 旧 HistoryStore 历史 → Room，完成后清空旧键。
     */
    suspend fun migrateFrom(settings: SettingsStore, historyStore: HistoryStore) {
        runCatching {
            settings.exportBookmarks().forEach { (title, url) -> addBookmark(title, url) }
            historyStore.exportHistory().forEach { entry ->
                if (entry.url.startsWith("http")) {
                    historyDao.insert(
                        HistoryEntity(title = entry.title.take(120), url = entry.url, time = entry.time),
                    )
                }
            }
            settings.clearBookmarksKey()
            historyStore.clearLegacyHistoryKey()
        }
    }
}
