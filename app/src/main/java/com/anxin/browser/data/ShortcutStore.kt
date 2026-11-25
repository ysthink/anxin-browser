package com.anxin.browser.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shortcutDataStore by preferencesDataStore(name = "anxin_shortcuts")

data class Shortcut(
    val label: String,
    val url: String,
    val badge: String,
    val color: Long,
)

/**
 * 用户主动添加的常用网站。只存用户数据，不生成任何推荐内容。
 */
class ShortcutStore(private val context: Context) {

    private val key = stringPreferencesKey("shortcuts_v1")

    val shortcuts: Flow<List<Shortcut>> = context.shortcutDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            raw.split("\n").mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size == 4) {
                    runCatching {
                        Shortcut(parts[0], parts[1], parts[2], parts[3].toLong())
                    }.getOrNull()
                } else null
            }
        }?.take(12) ?: defaults
    }

    suspend fun add(label: String, url: String) {
        val cleanLabel = label.trim().replace(Regex("[|\n]"), "").take(12)
        val cleanUrl = url.trim().replace(Regex("[|\n]"), "")
        if (cleanLabel.isBlank() || cleanUrl.isBlank()) return
        context.shortcutDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                raw.split("\n").mapNotNull { line ->
                    val parts = line.split("|", limit = 4)
                    if (parts.size == 4) {
                        runCatching { Shortcut(parts[0], parts[1], parts[2], parts[3].toLong()) }.getOrNull()
                    } else null
                }
            } ?: defaults
            if (current.any { it.url == cleanUrl || it.label == cleanLabel }) return@edit
            val next = (current + Shortcut(
                label = cleanLabel,
                url = normalizeInput(cleanUrl),
                badge = cleanLabel.take(1).uppercase(),
                color = palette[current.size % palette.size],
            )).take(12)
            prefs[key] = next.joinToString("\n") { "${it.label}|${it.url}|${it.badge}|${it.color}" }
        }
    }

    suspend fun remove(url: String) {
        context.shortcutDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                raw.split("\n").mapNotNull { line ->
                    val parts = line.split("|", limit = 4)
                    if (parts.size == 4) {
                        runCatching { Shortcut(parts[0], parts[1], parts[2], parts[3].toLong()) }.getOrNull()
                    } else null
                }
            } ?: defaults
            val next = current.filterNot { it.url == url }
            prefs[key] = next.joinToString("\n") { "${it.label}|${it.url}|${it.badge}|${it.color}" }
        }
    }

    suspend fun reset() {
        context.shortcutDataStore.edit { it.remove(key) }
    }

    private fun normalizeInput(input: String): String =
        if (input.contains("://") || input.startsWith("about:")) input else "https://$input"

    companion object {
        private val palette = listOf(
            0xFF2B34E8L, 0xFFFF5A1FL, 0xFFFB7299L, 0xFFE1251BL,
            0xFF23C08AL, 0xFF7B5CFFL, 0xFF3D6BFFL, 0xFF5B6BB5L,
        )

        val defaults = listOf(
            Shortcut("百度", "https://www.baidu.com", "百", 0xFF2B34E8L),
            Shortcut("淘宝", "https://www.taobao.com", "淘", 0xFFFF5A1FL),
            Shortcut("bilibili", "https://www.bilibili.com", "B", 0xFFFB7299L),
            Shortcut("京东", "https://www.jd.com", "京", 0xFFE1251BL),
        )
    }
}
