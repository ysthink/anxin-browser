package com.anxin.browser.data.extension

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anxin.browser.core.util.Urls
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.extensionDataStore by preferencesDataStore(name = "anxin_extensions")

/**
 * 类扩展实现：本地脚本包 + 站点匹配注入。
 *
 * 支持两种导入格式：
 * 1. zip 包：根目录 manifest.json { name, version, description, matches[], run_at, script }
 *    + 对应脚本文件（默认 script.js，也接受包内唯一的 *.user.js）；
 * 2. .user.js 单文件（解析 ==UserScript== 头部的 @name/@match/@version/@description/@run-at）。
 *
 * 注入：scriptsFor(url) 返回带窗口去重包装的脚本，由 WebViewEngine 在页面加载完成后执行，
 * 不依赖任何内核扩展 API。
 */
class ScriptExtensionBridge(private val context: Context) : ExtensionKernelBridge {

    private val key = stringPreferencesKey("extensions_v1")

    private val _extensions = MutableStateFlow<List<UserExtension>>(emptyList())
    override fun observe(): StateFlow<List<UserExtension>> = _extensions.asStateFlow()

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO,
    )

    init {
        // 冷启动从 DataStore 载入
        scope.launch {
            val raw = context.extensionDataStore.data.map { it[key] }.first()
            _extensions.value = decode(raw)
        }
    }

    private suspend fun persist(next: List<UserExtension>) {
        context.extensionDataStore.edit { prefs ->
            val array = JSONArray()
            next.forEach { ext ->
                array.put(
                    JSONObject()
                        .put("id", ext.id)
                        .put("name", ext.name)
                        .put("version", ext.version)
                        .put("description", ext.description)
                        .put("script", ext.script)
                        .put("matches", JSONArray(ext.matches))
                        .put("runAt", ext.runAt)
                        .put("enabled", ext.enabled)
                        .put("pinned", ext.pinned)
                        .put("sourceName", ext.sourceName)
                        .put("installedAt", ext.installedAt),
                )
            }
            prefs[key] = array.toString()
        }
        _extensions.value = next
    }

    private fun decode(raw: String?): List<UserExtension> = runCatching {
        val array = JSONArray(raw ?: "[]")
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val matches = mutableListOf<String>()
            val mArr = obj.optJSONArray("matches")
            if (mArr != null) {
                for (j in 0 until mArr.length()) matches += mArr.optString(j)
            }
            UserExtension(
                id = obj.optString("id"),
                name = obj.optString("name"),
                version = obj.optString("version", "1.0"),
                description = obj.optString("description"),
                script = obj.optString("script"),
                matches = matches.ifEmpty { listOf("<all_urls>") },
                runAt = obj.optString("runAt", "document_end"),
                enabled = obj.optBoolean("enabled", true),
                pinned = obj.optBoolean("pinned", false),
                sourceName = obj.optString("sourceName"),
                installedAt = obj.optLong("installedAt"),
            )
        }
    }.getOrDefault(emptyList())

    override suspend fun importFromFile(path: String, sourceName: String): UserExtension? {
        val file = File(path)
        if (!file.exists()) return null
        val lower = sourceName.lowercase()
        val extension: UserExtension = when {
            lower.endsWith(".user.js") -> parseUserScript(file, sourceName)
            lower.endsWith(".zip") || lower.endsWith(".crx") -> parseZip(file, sourceName)
            else -> {
                // 内容嗅探：先按 zip 试，再按 user.js 试
                parseZip(file, sourceName) ?: parseUserScript(file, sourceName)
            }
        } ?: return null

        val current = _extensions.value
        if (current.size >= UserExtension.MAX_COUNT) return null
        // 同名覆盖升级
        val next = current.filterNot { it.name == extension.name } + extension
        persist(next)
        return extension
    }

    override suspend fun remove(id: String) {
        persist(_extensions.value.filterNot { it.id == id })
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        persist(_extensions.value.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        persist(_extensions.value.map { if (it.id == id) it.copy(pinned = pinned) else it })
    }

    /** 返回该 URL 应注入的脚本（带去重包装，避免 SPA 重复执行）。 */
    fun scriptsFor(url: String): List<String> =
        _extensions.value
            .filter { it.enabled && it.script.isNotBlank() && urlMatches(url, it.matches) }
            .map { ext ->
                val flag = "__anxin_ext_${ext.id}"
                "(function(){ try{ if(window['$flag']) return; window['$flag']=1; }catch(e){}\n" +
                    ext.script + "\n})();"
            }

    // ---------- 解析 ----------

    private fun parseZip(file: File, sourceName: String): UserExtension? = runCatching {
        ZipFile(file).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json") ?: return@runCatching null
            val manifest = JSONObject(
                zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() },
            )
            val scriptName = manifest.optString("script", "script.js")
            var scriptEntry = zip.getEntry(scriptName)
            if (scriptEntry == null) {
                scriptEntry = zip.entries().asSequence()
                    .firstOrNull { it.name.endsWith(".user.js") || it.name == "script.js" }
            }
            if (scriptEntry == null) return@runCatching null
            val script = zip.getInputStream(scriptEntry).bufferedReader().use { it.readText() }
                .take(UserExtension.MAX_SCRIPT_CHARS)
            if (script.isBlank()) return@runCatching null
            UserExtension(
                id = stableId(manifest.optString("name", sourceName) + script.hashCode()),
                name = manifest.optString("name", sourceName.removeSuffix(".zip")).take(40),
                version = manifest.optString("version", "1.0").take(20),
                description = manifest.optString("description").take(200),
                script = script,
                matches = stringList(manifest.optJSONArray("matches")).ifEmpty { listOf("<all_urls>") },
                runAt = manifest.optString("run_at", "document_end").take(20),
                sourceName = sourceName,
            )
        }
    }.getOrNull()

    private fun parseUserScript(file: File, sourceName: String): UserExtension? = runCatching {
        val content = file.bufferedReader().use { it.readText() }.take(UserExtension.MAX_SCRIPT_CHARS)
        if (content.isBlank()) return@runCatching null
        var name = sourceName.removeSuffix(".user.js")
        var version = "1.0"
        var description = ""
        val matches = mutableListOf<String>()
        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("//")) return@forEach
            val body = trimmed.removePrefix("//").trim()
            val key = body.substringBefore(' ').trim()
            val value = body.substringAfter(' ', "").trim()
            when (key) {
                "@name" -> if (value.isNotBlank()) name = value.take(40)
                "@version" -> if (value.isNotBlank()) version = value.take(20)
                "@description" -> if (value.isNotBlank()) description = value.take(200)
                "@match", "@include" -> if (value.isNotBlank()) matches += value.take(200)
            }
        }
        UserExtension(
            id = stableId(name + content.hashCode()),
            name = name,
            version = version,
            description = description,
            script = content,
            matches = matches.ifEmpty { listOf("<all_urls>") },
            sourceName = sourceName,
        )
    }.getOrNull()

    private fun stringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return List(array.length()) { array.optString(it) }
    }

    private fun stableId(seed: String): String =
        java.security.MessageDigest.getInstance("MD5")
            .digest(seed.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    // ---------- 站点匹配 ----------

    fun urlMatches(url: String, patterns: List<String>): Boolean =
        patterns.any { matchesPattern(url, it) }

    private fun matchesPattern(url: String, pattern: String): Boolean {
        if (pattern == "<all_urls>") return true
        if (!url.startsWith("http")) return false
        val parts = pattern.split("://", limit = 2)
        if (parts.size != 2) return false
        val hostPattern = parts[1].substringBefore('/')
        val pathPattern = parts[1].substringAfter('/', "")
        val urlHost = Urls.hostOf(url)
        if (urlHost.isBlank()) return false
        if (!globToRegex(hostPattern).matches(urlHost)) return false
        val path = Uri.parse(url).let { (it.path ?: "/") + (it.query?.let { q -> "?$q" } ?: "") }
        return globToRegex(pathPattern).find(path) != null
    }

    private fun globToRegex(pattern: String) =
        Regex("^" + pattern.split("*").joinToString(".*") { Regex.escape(it) })
}
