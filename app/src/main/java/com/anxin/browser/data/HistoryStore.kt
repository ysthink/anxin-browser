package com.anxin.browser.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anxin.browser.core.util.Urls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.historyDataStore by preferencesDataStore(name = "anxin_history")

data class HistoryEntry(
    val title: String,
    val url: String,
    val time: Long,
)

/**
 * 浏览历史 + 搜索历史 + 经常访问。
 *
 * - 无痕标签由调用方过滤，不入库；
 * - 文本行编码：time|title|url，标题与地址去除分隔符；
 * - 内部自带 IO 协程，写入方无需 suspend。
 */
class HistoryStore(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val historyKey = stringPreferencesKey("history_v1")
    private val searchKey = stringPreferencesKey("searches_v1")

    private fun decode(raw: String?): List<HistoryEntry> =
        raw?.split("\n").orEmpty().mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                runCatching {
                    HistoryEntry(parts[1], parts[2], parts[0].toLong())
                }.getOrNull()
            } else null
        }

    private fun encode(entries: List<HistoryEntry>): String =
        entries.joinToString("\n") { entry ->
            val title = entry.title.replace(Regex("[|\n]"), " ").take(120)
            val url = entry.url.replace(Regex("[|\n]"), "")
            "${entry.time}|$title|$url"
        }

    /** 浏览历史（最新在前）。 */
    val history: Flow<List<HistoryEntry>> = context.historyDataStore.data.map {
        decode(it[historyKey])
    }

    /** 搜索历史（最新在前）。 */
    val searches: Flow<List<String>> = context.historyDataStore.data.map {
        decode(it[searchKey]).map { e -> e.title }
    }

    /** 经常访问：按域名聚合历史，取访问次数最多的前 [limit] 个站点。 */
    fun frequentlyVisited(limit: Int = 4): Flow<List<HistoryEntry>> = history.map { current ->
        if (current.isEmpty()) {
            emptyList()
        } else {
            val httpOnly = current.filter {
                it.url.startsWith("http://") || it.url.startsWith("https://")
            }
            httpOnly
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
    }

    /** 记录一次访问（内部 IO，不阻塞调用方）。 */
    fun record(title: String, url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        scope.launch {
            context.historyDataStore.edit { prefs ->
                val entries = decode(prefs[historyKey])
                    .filterNot { it.url == url }
                prefs[historyKey] = encode(
                    (listOf(HistoryEntry(title, url, System.currentTimeMillis())) + entries).take(300),
                )
            }
        }
    }

    fun recordSearch(query: String) {
        val clean = query.trim().take(80)
        if (clean.isEmpty()) return
        scope.launch {
            context.historyDataStore.edit { prefs ->
                val entries = decode(prefs[searchKey])
                    .filterNot { it.title == clean }
                prefs[searchKey] = encode(
                    (listOf(HistoryEntry(clean, "", System.currentTimeMillis())) + entries).take(50),
                )
            }
        }
    }

    fun removeHistory(url: String) {
        scope.launch {
            context.historyDataStore.edit { prefs ->
                prefs[historyKey] = encode(decode(prefs[historyKey]).filterNot { it.url == url })
            }
        }
    }

    fun clearHistory() {
        scope.launch { context.historyDataStore.edit { it.remove(historyKey) } }
    }

    /** 旧版浏览历史导出（Room 迁移用）。 */
    suspend fun exportHistory(): List<HistoryEntry> =
        context.historyDataStore.data.map { decode(it[historyKey]) }.first()

    /** 清空旧历史键（Room 迁移完成后调用）。 */
    suspend fun clearLegacyHistoryKey() {
        context.historyDataStore.edit { it.remove(historyKey) }
    }

    fun clearSearches() {
        scope.launch { context.historyDataStore.edit { it.remove(searchKey) } }
    }

    fun clearAll() {
        scope.launch {
            context.historyDataStore.edit {
                it.remove(historyKey)
                it.remove(searchKey)
            }
        }
    }
}
