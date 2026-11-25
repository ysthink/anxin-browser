package com.anxin.browser.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anxin.browser.core.model.BrowserTab
import com.anxin.browser.core.model.TabSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.sessionDataStore by preferencesDataStore(name = "anxin_session")

/**
 * 标签会话持久化：
 * - 只保存普通标签（无痕标签不跨进程恢复）；
 * - 保存 url + activeIndex，恢复时由内核懒加载非活动标签。
 */
class SessionStore(private val context: Context) {

    private val key = stringPreferencesKey("session_v1")

    /** 读取上次会话；无记录或内容非法返回 null。 */
    suspend fun read(): Pair<List<TabSession>, Int>? = runCatching {
        val raw = context.sessionDataStore.data.map { it[key] }.first() ?: return null
        val obj = JSONObject(raw)
        val array = obj.optJSONArray("tabs") ?: return null
        val sessions = mutableListOf<TabSession>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.startsWith("http://") || url.startsWith("https://")) {
                sessions += TabSession(url, item.optBoolean("incognito"))
            }
        }
        if (sessions.isEmpty()) return null
        sessions to obj.optInt("active", 0)
    }.getOrNull()

    /** 保存当前标签会话（普通 http(s) 标签）。 */
    suspend fun save(tabs: List<BrowserTab>, activeTabId: String?) {
        val sessions = tabs.filter { tab ->
            !tab.isIncognito && (tab.url.startsWith("http://") || tab.url.startsWith("https://"))
        }
        val activeIndex = sessions.indexOfFirst { it.url == tabs.firstOrNull { t -> t.id == activeTabId }?.url }
        context.sessionDataStore.edit { prefs ->
            if (sessions.isEmpty()) {
                prefs.remove(key)
            } else {
                val array = JSONArray()
                sessions.forEach { tab ->
                    array.put(JSONObject().put("url", tab.url).put("incognito", tab.isIncognito))
                }
                prefs[key] = JSONObject()
                    .put("tabs", array)
                    .put("active", if (activeIndex >= 0) activeIndex else 0)
                    .toString()
            }
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.remove(key) }
    }
}
