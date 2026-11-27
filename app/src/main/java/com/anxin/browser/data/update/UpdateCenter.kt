package com.anxin.browser.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.anxin.browser.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 应用内更新（FlClash 式：GitHub Releases 打底）。
 *
 * 发版约定：
 *  - tag 形如 v0.3.8，Release 资产命名 anxin-v0.3.8-release.apk + SHA256SUMS
 *  - Release 描述第一行嵌入元数据注释（CI 自动生成）：
 *      <!--ANXIN-UPDATE {"versionCode":11,"sha256":"…","forceUpdate":false}-->
 *    注释之后正文即"更新说明"，原样展示给用户。
 *
 * 状态机：Idle → Checking → UpToDate | Available → Downloading → ReadyToInstall
 *         任何一步失败进 Failed（可重试）。
 */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val received: Long, val total: Long) : UpdateUiState
    data class ReadyToInstall(val info: UpdateInfo, val apk: File) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String,
    val forceUpdate: Boolean,
)

object UpdateCenter {

    const val REPO = "ysthink/anxin-browser"
    private const val RELEASES_LATEST = "https://api.github.com/repos/$REPO/releases/latest"
    private const val MARKER = "<!--ANXIN-UPDATE"
    private const val APK_NAME = "anxin-update.apk"
    private const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    private const val PREFS = "update_center"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var lastCheckMs = 0L

    // ------------------------------------------------------------------
    // 对外动作
    // ------------------------------------------------------------------

    /** 用户手动检查：无视节流。 */
    fun check() {
        scope.launch { checkInternal(force = true) }
    }

    /** 启动静默检查：24h 节流，失败静默不打扰。 */
    fun autoCheckIfNeeded(context: Context) {
        scope.launch {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (now - prefs.getLong("last_auto_check", 0L) < AUTO_CHECK_INTERVAL_MS) return@launch
            prefs.edit().putLong("last_auto_check", now).apply()
            runCatching { checkInternal(force = false) }
        }
    }

    fun startDownload(context: Context) {
        val info = (_state.value as? UpdateUiState.Available)?.info ?: return
        scope.launch {
            _state.value = UpdateUiState.Downloading(info, 0, 0)
            runCatching {
                val file = apkFile(context)
                file.parentFile?.mkdirs()
                download(info.apkUrl, file) { received, total ->
                    if (total > 0) {
                        _state.value = UpdateUiState.Downloading(info, received, total)
                    }
                }
                info.sha256.takeIf { it.length == 64 }?.let { expected ->
                    val actual = sha256Hex(file)
                    if (!actual.equals(expected, ignoreCase = true)) {
                        file.delete()
                        throw IllegalStateException("安装包校验失败（SHA256 不匹配），已删除，请重试")
                    }
                }
                _state.value = UpdateUiState.ReadyToInstall(info, file)
            }.onFailure { e ->
                _state.value = UpdateUiState.Failed(e.message ?: "下载失败")
            }
        }
    }

    fun reset() {
        _state.value = UpdateUiState.Idle
    }

    // ------------------------------------------------------------------
    // 安装
    // ------------------------------------------------------------------

    fun buildInstallIntent(context: Context, apk: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${BuildConfig.APPLICATION_ID}.fileprovider", apk,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** 覆盖安装完成后（或用户放弃）清掉缓存的 APK。 */
    fun clearDownload(context: Context) {
        runCatching { apkFile(context).delete() }
    }

    private fun apkFile(context: Context): File {
        val dir = context.getExternalFilesDir("update") ?: File(context.cacheDir, "update")
        return File(dir, APK_NAME)
    }

    // ------------------------------------------------------------------
    // 检查
    // ------------------------------------------------------------------

    private suspend fun checkInternal(force: Boolean) = withContext(Dispatchers.IO) {
        val current = _state.value
        // 正在下载/待安装时不要把状态打回 Checking
        if (!force && current is UpdateUiState.Downloading) return@withContext
        _state.value = UpdateUiState.Checking
        runCatching { fetchLatest() }
            .onSuccess { info ->
                lastCheckMs = System.currentTimeMillis()
                _state.value =
                    if (info != null && info.versionCode > BuildConfig.VERSION_CODE) {
                        UpdateUiState.Available(info)
                    } else {
                        UpdateUiState.UpToDate
                    }
            }
            .onFailure { e ->
                if (force) {
                    _state.value = UpdateUiState.Failed(e.message ?: "网络异常，请稍后重试")
                } else {
                    // 静默检查失败回到空闲，不打扰用户
                    _state.value = UpdateUiState.Idle
                }
            }
    }

    /** 拉取最新 Release 并解析；无新版本返回 null。 */
    private fun fetchLatest(): UpdateInfo? {
        val conn = (URL(RELEASES_LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "anxin-browser")
        }
        try {
            if (conn.responseCode != 200) {
                throw IllegalStateException("检查更新失败（HTTP ${conn.responseCode}）")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").removePrefix("v")
            val releaseBody = json.optString("body").orEmpty()
            val meta = parseMeta(releaseBody)
                ?: throw IllegalStateException("版本元数据缺失，请稍后重试")

            // 资产匹配：优先按约定命名，兜底第一个 .apk
            val assets = json.optJSONArray("assets") ?: throw IllegalStateException("版本资产缺失")
            var url: String? = null
            val preferred = "anxin-v$tag-release.apk"
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name == preferred) {
                    url = a.optString("browser_download_url")
                    break
                }
            }
            if (url == null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) {
                        url = a.optString("browser_download_url")
                        break
                    }
                }
            }
            url ?: throw IllegalStateException("未找到可下载的安装包")

            return UpdateInfo(
                versionCode = meta.first,
                versionName = tag,
                apkUrl = url,
                sha256 = meta.second,
                notes = releaseNotes(releaseBody),
                forceUpdate = meta.third,
            )
        } finally {
            conn.disconnect()
        }
    }

    /** 解析 body 里的 ANXIN-UPDATE 元数据：versionCode / sha256 / forceUpdate。 */
    private fun parseMeta(releaseBody: String): Triple<Int, String, Boolean>? {
        val start = releaseBody.indexOf(MARKER)
        if (start < 0) return null
        val commentEnd = releaseBody.indexOf("-->", start)
        if (commentEnd < 0) return null
        val objStart = releaseBody.indexOf('{', start)
        val objEnd = releaseBody.lastIndexOf('}', commentEnd)
        if (objStart < 0 || objEnd <= objStart) return null
        return runCatching {
            val obj = JSONObject(releaseBody.substring(objStart, objEnd + 1))
            Triple(
                obj.optInt("versionCode", 0),
                obj.optString("sha256"),
                obj.optBoolean("forceUpdate", false),
            )
        }.getOrNull()
    }

    /** 元数据注释之后的正文，原样作为更新说明。 */
    private fun releaseNotes(releaseBody: String): String {
        val end = releaseBody.indexOf("-->")
        return if (end >= 0) releaseBody.substring(end + 3).trim() else ""
    }

    // ------------------------------------------------------------------
    // 下载 / 校验
    // ------------------------------------------------------------------

    private fun download(url: String, target: File, onProgress: (Long, Long) -> Unit) {
        val tmp = File(target.parentFile, target.name + ".part")
        tmp.delete()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "anxin-browser")
        }
        try {
            if (conn.responseCode != 200) {
                throw IllegalStateException("下载失败（HTTP ${conn.responseCode}）")
            }
            val total = conn.contentLengthLong
            var received = 0L
            var lastPercent = -1
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        val percent = if (total > 0) (received * 100 / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(received, total)
                        }
                    }
                }
            }
            if (total > 0 && received != total) {
                throw IllegalStateException("下载不完整（$received/$total），请重试")
            }
            if (!tmp.renameTo(target)) {
                throw IllegalStateException("安装包保存失败，请重试")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
