package com.anxin.browser.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anxin.browser.core.util.Urls
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "anxin_settings")

/** 设置项持久化（DataStore），后续可平滑替换为 Room 配置表。 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val adBlock = booleanPreferencesKey("ad_block")
        val doNotTrack = booleanPreferencesKey("do_not_track")
        val wifiOnlyDownload = booleanPreferencesKey("wifi_only_download")
        val pipEnabled = booleanPreferencesKey("pip_enabled")
        val searchEngine = stringPreferencesKey("search_engine")
        val engineBar = booleanPreferencesKey("search_engine_bar")
        val bookmarks = stringPreferencesKey("bookmarks_v1")
        val playbackSpeed = intPreferencesKey("playback_speed_percent")
        val signedIn = booleanPreferencesKey("signed_in")
        // 网页浏览设置
        val textZoom = intPreferencesKey("text_zoom_percent")
        val desktopMode = booleanPreferencesKey("desktop_mode")
        val nightMode = booleanPreferencesKey("night_mode")
        val fullscreenMode = booleanPreferencesKey("fullscreen_mode")
        // 辅助
        val exitConfirm = booleanPreferencesKey("exit_confirm")
        // 主页设置
        val homepageMode = stringPreferencesKey("homepage_mode")
        val customHomepage = stringPreferencesKey("custom_homepage")
        val wallpaper = intPreferencesKey("wallpaper_index")
        // 播放设置
        val playDynamicVideo = booleanPreferencesKey("play_dynamic_video")
        val backgroundPlay = booleanPreferencesKey("background_play")
        val autoMiniWindow = booleanPreferencesKey("auto_mini_window")
        // 菜单布局：JSON 数组 [{id, visible}]
        val menuLayout = stringPreferencesKey("menu_layout_v1")
    }

    val adBlockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.adBlock] ?: true }
    val doNotTrack: Flow<Boolean> = context.dataStore.data.map { it[Keys.doNotTrack] ?: true }
    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { it[Keys.wifiOnlyDownload] ?: false }
    val pipEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.pipEnabled] ?: true }
    val playbackSpeedPercent: Flow<Int> = context.dataStore.data.map { it[Keys.playbackSpeed] ?: 100 }
    val signedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.signedIn] ?: false }
    val searchEngine: Flow<String> = context.dataStore.data.map { Urls.searchEngineName(it[Keys.searchEngine]) }

    val showEngineBar: Flow<Boolean> = context.dataStore.data.map { it[Keys.engineBar] ?: true }
    val bookmarks: Flow<List<Pair<String, String>>> = context.dataStore.data.map { decodeBookmarks(it[Keys.bookmarks]) }

    // 网页浏览设置
    val textZoomPercent: Flow<Int> = context.dataStore.data.map { it[Keys.textZoom] ?: 100 }
    val desktopMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.desktopMode] ?: false }
    val nightMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.nightMode] ?: false }
    val fullscreenMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.fullscreenMode] ?: false }

    // 辅助
    val exitConfirm: Flow<Boolean> = context.dataStore.data.map { it[Keys.exitConfirm] ?: true }

    // 主页设置
    val homepageMode: Flow<String> = context.dataStore.data.map { it[Keys.homepageMode] ?: "default" }
    val customHomepage: Flow<String> = context.dataStore.data.map { it[Keys.customHomepage] ?: "" }
    val wallpaperIndex: Flow<Int> = context.dataStore.data.map { it[Keys.wallpaper] ?: 0 }

    // 播放设置
    val playDynamicVideo: Flow<Boolean> = context.dataStore.data.map { it[Keys.playDynamicVideo] ?: false }
    val backgroundPlay: Flow<Boolean> = context.dataStore.data.map { it[Keys.backgroundPlay] ?: true }
    val autoMiniWindow: Flow<Boolean> = context.dataStore.data.map { it[Keys.autoMiniWindow] ?: false }

    // 菜单布局
    val menuLayout: Flow<List<MenuEntry>> = context.dataStore.data.map { decodeMenu(it[Keys.menuLayout]) }

    private fun decodeMenu(raw: String?): List<MenuEntry> {
        val parsed = runCatching {
            val array = org.json.JSONArray(raw ?: "[]")
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                MenuEntry(obj.getString("id"), obj.optBoolean("visible", true))
            }
        }.getOrDefault(emptyList())
        if (parsed.isEmpty()) return MenuEntry.defaultLayout()
        // 补齐新增菜单项，保持用户自定义顺序优先。
        val known = parsed.map { it.id }.toSet()
        val merged = parsed + MenuEntry.defaultLayout().filterNot { it.id in known }
        return merged
    }

    private fun decodeBookmarks(raw: String?): List<Pair<String, String>> = runCatching {
        val array = org.json.JSONArray(raw ?: "[]")
        List(array.length()) { i -> array.getJSONArray(i).let { it.getString(0) to it.getString(1) } }
    }.getOrDefault(emptyList())

    suspend fun addBookmark(title: String, url: String) = context.dataStore.edit { prefs ->
        val entries = decodeBookmarks(prefs[Keys.bookmarks])
        prefs[Keys.bookmarks] = org.json.JSONArray().apply {
            ((title.ifBlank { url } to url).let { listOf(it) } + entries.filterNot { it.second == url }).forEach { (name, address) ->
                put(org.json.JSONArray().put(name).put(address))
            }
        }.toString()
    }

    suspend fun removeBookmark(url: String) = context.dataStore.edit { prefs ->
        prefs[Keys.bookmarks] = encodeBookmarks(decodeBookmarks(prefs[Keys.bookmarks]).filterNot { it.second == url })
    }

    private fun encodeBookmarks(entries: List<Pair<String, String>>): String = org.json.JSONArray().apply {
        entries.forEach { (name, address) ->
            put(org.json.JSONArray().put(name).put(address))
        }
    }.toString()

    /** 旧版书签导出（Room 迁移用）。 */
    suspend fun exportBookmarks(): List<Pair<String, String>> =
        context.dataStore.data.map { decodeBookmarks(it[Keys.bookmarks]) }.first()

    /** 清空旧书签键（Room 迁移完成后调用）。 */
    suspend fun clearBookmarksKey() = context.dataStore.edit { it.remove(Keys.bookmarks) }

    suspend fun setEngineBar(visible: Boolean) = context.dataStore.edit { it[Keys.engineBar] = visible }

    suspend fun setAdBlock(enabled: Boolean) = context.dataStore.edit { it[Keys.adBlock] = enabled }
    suspend fun setDoNotTrack(enabled: Boolean) = context.dataStore.edit { it[Keys.doNotTrack] = enabled }
    suspend fun setWifiOnly(enabled: Boolean) = context.dataStore.edit { it[Keys.wifiOnlyDownload] = enabled }
    suspend fun setPip(enabled: Boolean) = context.dataStore.edit { it[Keys.pipEnabled] = enabled }
    suspend fun setPlaybackSpeed(percent: Int) = context.dataStore.edit { it[Keys.playbackSpeed] = percent }
    suspend fun setSignedIn(value: Boolean) = context.dataStore.edit { it[Keys.signedIn] = value }
    suspend fun setSearchEngine(value: String) = context.dataStore.edit { it[Keys.searchEngine] = value }

    suspend fun setTextZoom(percent: Int) = context.dataStore.edit { it[Keys.textZoom] = percent }
    suspend fun setDesktopMode(enabled: Boolean) = context.dataStore.edit { it[Keys.desktopMode] = enabled }
    suspend fun setNightMode(enabled: Boolean) = context.dataStore.edit { it[Keys.nightMode] = enabled }
    suspend fun setFullscreenMode(enabled: Boolean) = context.dataStore.edit { it[Keys.fullscreenMode] = enabled }
    suspend fun setExitConfirm(enabled: Boolean) = context.dataStore.edit { it[Keys.exitConfirm] = enabled }
    suspend fun setHomepageMode(mode: String) = context.dataStore.edit { it[Keys.homepageMode] = mode }
    suspend fun setCustomHomepage(url: String) = context.dataStore.edit { it[Keys.customHomepage] = url }
    suspend fun setWallpaperIndex(index: Int) = context.dataStore.edit { it[Keys.wallpaper] = index }
    suspend fun setPlayDynamicVideo(enabled: Boolean) = context.dataStore.edit { it[Keys.playDynamicVideo] = enabled }
    suspend fun setBackgroundPlay(enabled: Boolean) = context.dataStore.edit { it[Keys.backgroundPlay] = enabled }
    suspend fun setAutoMiniWindow(enabled: Boolean) = context.dataStore.edit { it[Keys.autoMiniWindow] = enabled }

    suspend fun setMenuLayout(entries: List<MenuEntry>) = context.dataStore.edit { prefs ->
        val array = org.json.JSONArray()
        entries.forEach { entry ->
            array.put(
                org.json.JSONObject()
                    .put("id", entry.id)
                    .put("visible", entry.visible),
            )
        }
        prefs[Keys.menuLayout] = array.toString()
    }

    /** 恢复默认设置：清除所有持久化配置，保留用户数据（书签/历史/下载）。 */
    suspend fun resetAll() = context.dataStore.edit { it.clear() }
}

/** 菜单面板项：id 对应 MenuSheet 中的动作映射。 */
data class MenuEntry(
    val id: String,
    val visible: Boolean = true,
) {
    companion object {
        /** 菜单默认顺序（对齐功能梳理的 6 行网格）。 */
        fun defaultLayout(): List<MenuEntry> = listOf(
            MenuEntry("bookmark"), MenuEntry("history"), MenuEntry("downloads"),
            MenuEntry("videos"), MenuEntry("night"),
            MenuEntry("incognito"), MenuEntry("addBookmark"), MenuEntry("refresh"),
            MenuEntry("exit"), MenuEntry("reading"),
            MenuEntry("images"), MenuEntry("fullscreen"), MenuEntry("desktop"),
            MenuEntry("extensions"),
            MenuEntry("sniff"), MenuEntry("translate"), MenuEntry("addToDesktop"),
            MenuEntry("find"), MenuEntry("saveOffline"),
            MenuEntry("textSize"), MenuEntry("admark"), MenuEntry("print"),
            MenuEntry("zoom"),
        )
    }
}
