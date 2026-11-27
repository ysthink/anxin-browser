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
            .map { ext -> wrapScript(ext) }

    /**
     * 把用户脚本包进一个带 `GM_*` 兼容层的 IIFE。
     *
     * 为什么要这一层：油猴脚本的第一行几乎都是 `GM_setValue` / `GM_addStyle`，
     * 没有垫片就会直接抛 `ReferenceError`，整个脚本一行都不执行。
     *
     * 用 IIFE 而不是把函数挂到 window 上，是为了让不同脚本的变量互不污染
     * （多个脚本同时声明 `var timer` 会互相踩）。存储命名空间按扩展 id 隔离，
     * 卸载重装同一个脚本仍能读回自己的数据。
     *
     * 已知边界：`GM_xmlhttpRequest` 走 `fetch`，跨域仍受 CORS 约束——
     * 没有 CORS 头的站点会走 `onerror`。这是 WebView 的能力边界，不是实现缺陷。
     */
    private fun wrapScript(ext: UserExtension): String {
        val flag = "__anxin_ext_${ext.id}"
        // 结构对齐油猴：脚本里普遍读 GM_info.script.name / GM_info.script.version。
        val meta = JSONObject().apply {
            put(
                "script",
                JSONObject().apply {
                    put("name", ext.name)
                    put("version", ext.version)
                    put("description", ext.description)
                },
            )
            put("scriptHandler", "安心浏览器")
        }.toString()

        return """
(function(){
  try{ if(window['$flag']) return; window['$flag']=1; }catch(e){}
  var __ns='anxin.gm.${ext.id}', __info=$meta;
  function __key(k){ return __ns+':'+k; }
  function GM_getValue(k, d){
    try{ var r=localStorage.getItem(__key(k)); return r===null?d:JSON.parse(r); }catch(e){ return d; }
  }
  function GM_setValue(k, v){
    try{ localStorage.setItem(__key(k), JSON.stringify(v)); }catch(e){}
  }
  function GM_deleteValue(k){ try{ localStorage.removeItem(__key(k)); }catch(e){} }
  function GM_listValues(){
    var out=[]; try{
      for(var i=0;i<localStorage.length;i++){
        var key=localStorage.key(i);
        if(key && key.indexOf(__ns+':')===0) out.push(key.substring(__ns.length+1));
      }
    }catch(e){}
    return out;
  }
  function GM_addStyle(css){
    var s=document.createElement('style');
    s.textContent=css;
    (document.head||document.documentElement).appendChild(s);
    return s;
  }
  function GM_log(){
    try{
      var n=(__info.script && __info.script.name) || '脚本';
      console.log.apply(console, ['['+n+']'].concat([].slice.call(arguments)));
    }catch(e){}
  }
  function GM_openInTab(u){
    // WebView 关闭了多窗口，window.open 通常会被拦；退化为当前页跳转。
    try{ var w=window.open(u,'_blank'); if(w) return {close:function(){try{w.close()}catch(e){}}}; }catch(e){}
    try{ location.href=u; }catch(e){}
    return null;
  }
  function GM_notification(o){
    try{
      if(typeof o === 'string') o={text:o};
      o=o||{};
      var title=o.title||(__info.script && __info.script.name)||'通知', body=o.text||'';
      if(window.Notification && Notification.permission==='granted'){
        new Notification(title,{body:body});
      }else{
        console.log('[通知] '+title+' '+body);
      }
    }catch(e){}
  }
  function GM_xmlhttpRequest(o){
    o=o||{};
    var aborted=false;
    try{
      fetch(o.url,{
        method:(o.method||'GET').toUpperCase(),
        headers:o.headers||{},
        body:o.data,
        credentials:'include'
      }).then(function(r){
        return r.text().then(function(t){
          if(aborted) return;
          if(o.onload) o.onload({
            status:r.status, statusText:r.statusText, responseText:t,
            finalUrl:r.url, readyState:4, responseHeaders:''
          });
        });
      }).catch(function(e){
        if(!aborted && o.onerror) o.onerror({status:0, error:e});
      });
    }catch(e){
      if(o.onerror) o.onerror({status:0, error:e});
    }
    return {abort:function(){ aborted=true; if(o.onabort) o.onabort(); }};
  }
  function GM_registerMenuCommand(name, fn){
    // 目前没有脚本菜单 UI，先存起来，避免脚本因缺少该函数而中断。
    try{ (window.__anxinMenuCommands=window.__anxinMenuCommands||[]).push({name:name, run:fn}); }catch(e){}
  }
  function GM_unregisterMenuCommand(){}
  function GM_addValueChangeListener(){ return 0; }
  function GM_removeValueChangeListener(){}
  var GM = {
    getValue:function(k,d){ return Promise.resolve(GM_getValue(k,d)); },
    setValue:function(k,v){ GM_setValue(k,v); return Promise.resolve(); },
    deleteValue:function(k){ GM_deleteValue(k); return Promise.resolve(); },
    listValues:function(){ return Promise.resolve(GM_listValues()); },
    addStyle:GM_addStyle,
    xmlHttpRequest:GM_xmlhttpRequest,
    notification:GM_notification,
    openInTab:GM_openInTab,
    registerMenuCommand:GM_registerMenuCommand,
    info:__info
  };
  var unsafeWindow = window;
  var GM_info = __info;
${ext.script}
})();
"""
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
