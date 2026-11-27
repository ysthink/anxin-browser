package com.anxin.browser.core.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import com.anxin.browser.core.model.DownloadItem
import com.anxin.browser.core.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * 下载中心数据源：自带 HTTP 断点续传的进程内下载器。
 *
 * 为什么不用系统 DownloadManager：
 * DownloadManager 没有暂停 API，`remove()` 会连临时文件一起删掉，所以"暂停"必然是假的。
 * 这里用 [HttpURLConnection] + `.part` 临时文件 + `Range` 请求头实现真正的暂停/继续：
 * 暂停只停止读取并保留已下载字节，继续时从断点续传。
 *
 * 文件落在应用私有外部目录（Downloads/），不需要任何存储权限；
 * 下载完成后 `localUri` 指向真实文件，UI 通过 FileProvider 转成 content:// 打开。
 *
 * 任务列表经 [store] 持久化（app 层用 Room 实现），进程被杀后重启仍能看到历史与断点：
 * 上次还在跑的任务恢复为"已暂停"，已下载字节以 `.part` 实际长度为准——
 * 落库的进度是节流的，永远可能落后于磁盘。
 */
class DownloadRepository(
    private val context: Context,
    private val store: DownloadStore = NoDownloadStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    /** 任务开始事件（文件名）。用于在页面层给用户"下载已开始"的即时反馈。 */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** 需要解释给用户听的信息（例如重复下载被拦下），不改变任何状态。 */
    private val _info = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val info: SharedFlow<String> = _info.asSharedFlow()

    private val jobs = HashMap<Long, Job>()
    private val paused = HashSet<Long>()
    private val meta = HashMap<Long, Meta>()
    private val nextId = AtomicLong(System.currentTimeMillis())

    private val dir: File by lazy {
        val d = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!d.exists()) d.mkdirs()
        d
    }

    private data class Meta(
        val userAgent: String?,
        val cookie: String?,
        val referer: String?,
    )

    /** 每个任务上次落库的时间，用于给高频进度回调做节流。 */
    private val lastPersist = HashMap<Long, Long>()

    /** 恢复历史任务。必须异步：Room 首次查询要走磁盘，不能卡住 Application.onCreate。 */
    private val restored = CompletableDeferred<Unit>()

    init {
        scope.launch { restore() }
    }

    /**
     * 从 [store] 载入历史任务。
     *
     * 两条必须遵守的规则：
     * 1) 上次处于 PENDING/RUNNING 的任务一律降级为 PAUSED——进程已经死过，
     *    没有任何连接还在传输，谎称"下载中"只会让进度条永远停在原地；
     * 2) 已下载字节以 `.part` 文件的实际长度为准，落库进度是节流的、天然滞后。
     */
    private suspend fun restore() {
        try {
            val records = runCatching { store.load() }.getOrDefault(emptyList())
            if (records.isEmpty()) return
        val known = _items.value.mapTo(HashSet()) { it.id }
        val recovered = records.mapNotNull { rec ->
            if (rec.item.id in known) return@mapNotNull null
            meta[rec.item.id] = Meta(rec.userAgent, rec.cookie, rec.referer)
            // 让后续新建任务的时间戳 id 一定大于历史 id，避免主键撞车。
            nextId.set(maxOf(nextId.get(), rec.item.id + 1))
            val item = rec.item
            when (item.status) {
                DownloadStatus.SUCCESS -> {
                    // 文件被删掉（用户清理、卸载重装、我们自己的测试）后，
                    // 这条记录唯一的用处就是"点开时提示文件不存在"——纯噪音，直接清掉。
                    if (!existsOnDisk(item)) {
                        scope.launch { runCatching { store.delete(item.id) } }
                        return@mapNotNull null
                    }
                    item.copy(downloadedBytes = maxOf(item.downloadedBytes, item.totalBytes))
                }
                DownloadStatus.PENDING, DownloadStatus.RUNNING -> {
                    val onDisk = runCatching { partFile(item).length() }.getOrDefault(0L)
                    paused += item.id
                    item.copy(status = DownloadStatus.PAUSED, downloadedBytes = onDisk)
                }
                DownloadStatus.PAUSED -> {
                    val onDisk = runCatching { partFile(item).length() }.getOrDefault(0L)
                    paused += item.id
                    item.copy(downloadedBytes = onDisk)
                }
                DownloadStatus.FAILED -> item
            }
        }
        if (recovered.isNotEmpty()) {
            _items.value = (recovered + _items.value).sortedBy { it.createdAt }
        }
        } finally {
            // 无论成功、失败还是提前返回，都要放行排队的入队请求。
            restored.complete(Unit)
        }
    }

    /**
     * 写库。[force] 为 true 用于状态变化（暂停/完成/失败），必须立刻可见；
     * 否则是进度回调，800ms 节流——每 180ms 一次进度写库会让 Room 持续占用 IO。
     */
    private fun persist(item: DownloadItem, force: Boolean) {
        // 无痕任务的记录不落库，只保留文件本身（与隐私说明一致）。
        if (item.fromIncognito) return
        val now = System.currentTimeMillis()
        if (!force && now - (lastPersist[item.id] ?: 0L) < 800L) return
        lastPersist[item.id] = now
        val m = meta[item.id]
        val record = DownloadRecord(
            item = item,
            userAgent = m?.userAgent,
            cookie = m?.cookie,
            referer = m?.referer,
        )
        scope.launch { runCatching { store.upsert(record) } }
    }

    fun enqueue(
        url: String,
        mimeType: String? = null,
        contentDisposition: String? = null,
        contentLength: Long = 0L,
        userAgent: String? = null,
        referer: String? = null,
        suggestedName: String? = null,
    ): Long? {
        // 历史任务还没恢复完时 _items 是空的，下面的去重闸门等于不存在。
        // 冷启动那一瞬间恰好有直链被加载（恢复的标签页、外部链接）就会重复入队，
        // 所以先排队，等恢复完成再走正常流程。
        if (!restored.isCompleted) {
            scope.launch {
                restored.await()
                enqueueNow(url, mimeType, contentDisposition, contentLength, userAgent, referer, suggestedName)
            }
            return null
        }
        return enqueueNow(url, mimeType, contentDisposition, contentLength, userAgent, referer, suggestedName)
    }

    private fun enqueueNow(
        url: String,
        mimeType: String?,
        contentDisposition: String?,
        contentLength: Long,
        userAgent: String?,
        referer: String?,
        suggestedName: String?,
    ): Long? {
        if (url.isBlank()) return null
        // data:/blob: 无法用 HTTP 拉取，交给页面自己处理（与系统浏览器一致）。
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null

        // 同一个 URL 已有未结束的任务时，复用而不是再建一条。
        //
        // 触发场景很具体：重启后恢复会话标签，那个直链 URL 会被 WebView 重新加载，
        // download listener 再触发一次——没有这道闸门就会凭空多出一条重复任务、
        // 甚至把文件下两遍。用户手抖连点两次链接也走同一条路径。
        // 不自动续传已暂停的任务：会话恢复不该在后台偷偷消耗流量，
        // 是否继续由用户在下载页决定。
        _items.value.firstOrNull {
            it.url == url &&
                (it.status == DownloadStatus.PENDING ||
                    it.status == DownloadStatus.RUNNING ||
                    it.status == DownloadStatus.PAUSED)
        }?.let { existing ->
            _info.tryEmit(
                when (existing.status) {
                    DownloadStatus.PAUSED -> "该文件已在下载列表中（已暂停），可在下载页继续"
                    else -> "该文件正在下载中：${existing.fileName}"
                }
            )
            return existing.id
        }
        val fileName = uniqueName(
            suggestedName ?: guessName(url, contentDisposition, mimeType)
        )
        val id = nextId.incrementAndGet()
        meta[id] = Meta(
            userAgent = userAgent,
            // CookieManager 需要在有 Looper 的线程调用更稳，这里在调用方线程同步取一次即可。
            cookie = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull(),
            referer = referer,
        )
        val item = DownloadItem(
            id = id,
            fileName = fileName,
            url = url,
            mimeType = mimeType,
            totalBytes = contentLength,
            status = DownloadStatus.PENDING,
        )
        _items.value = _items.value + item
        persist(item, force = true)
        start(item)
        _events.tryEmit(fileName)
        return id
    }

    /** 暂停：只停止读取，保留已下载字节与 `.part` 文件。 */
    fun pause(id: Long) {
        paused += id
        jobs.remove(id)?.cancel()
        update(id) { it.copy(status = DownloadStatus.PAUSED) }
    }

    /** 继续：从 `.part` 的当前长度作为 Range 起点续传。 */
    fun resume(id: Long) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        if (item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return
        paused -= id
        update(id) { it.copy(status = DownloadStatus.RUNNING) }
        start(item.copy(status = DownloadStatus.RUNNING))
    }

    /** 删除记录并清理该任务产生的临时文件（已完成的成品文件保留）。 */
    fun remove(id: Long) {
        paused -= id
        jobs.remove(id)?.cancel()
        _items.value.firstOrNull { it.id == id }?.let { item ->
            runCatching { partFile(item).delete() }
        }
        meta.remove(id)
        lastPersist.remove(id)
        _items.value = _items.value.filterNot { it.id == id }
        scope.launch { runCatching { store.delete(id) } }
    }

    /** 清空已结束的记录（不删除已下载的成品文件）。 */
    fun clearFinished() {
        val finished = _items.value.filter {
            it.status == DownloadStatus.SUCCESS || it.status == DownloadStatus.FAILED
        }
        finished.forEach {
            jobs.remove(it.id)?.cancel()
            meta.remove(it.id)
            lastPersist.remove(it.id)
        }
        _items.value = _items.value.filterNot {
            it.status == DownloadStatus.SUCCESS || it.status == DownloadStatus.FAILED
        }
        scope.launch { runCatching { store.clearFinished() } }
    }

    /** 是否有任务正在传输（前台服务据此决定是否保活）。 */
    val hasActive: Boolean
        get() = _items.value.any {
            it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PENDING
        }

    /** 兼容旧调用点：进度由下载协程主动推送，这里无事可做。 */
    fun refresh() = Unit

    // -----------------------------------------------------------------------

    private fun start(item: DownloadItem) {
        jobs.remove(item.id)?.cancel()
        jobs[item.id] = scope.launch { transfer(item) }
    }

    private suspend fun transfer(item: DownloadItem) {
        val part = partFile(item)
        var offset = if (part.exists()) part.length() else 0L

        update(item.id) { it.copy(status = DownloadStatus.RUNNING, downloadedBytes = offset) }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(item.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                meta[item.id]?.userAgent?.let { setRequestProperty("User-Agent", it) }
                meta[item.id]?.cookie?.let { setRequestProperty("Cookie", it) }
                meta[item.id]?.referer?.let { setRequestProperty("Referer", it) }
                if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
            }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK && offset > 0) {
                // 服务端不支持 Range：从头来，截断临时文件避免拼接出错。
                runCatching { RandomAccessFile(part, "rw").use { it.setLength(0) } }
                offset = 0
            } else if (code == 416) {
                // 已经下完了。
                finish(item, part, offset)
                return
            } else if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }

            val len = conn.contentLengthLong
            val total = if (code == 206) offset + len else len
            if (total > 0) update(item.id) { it.copy(totalBytes = total) }

            val ctx = currentCoroutineContext()
            RandomAccessFile(part, "rw").use { raf ->
                raf.seek(offset)
                var written = offset
                var lastEmit = 0L
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        // 必须显式检查：input.read 是阻塞调用，不会自己响应协程取消，
                        // 少了这一行"暂停"只是把状态改成暂停，字节还在往下写。
                        ctx.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        raf.write(buffer, 0, read)
                        written += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 180) {
                            lastEmit = now
                            val w = written
                            update(item.id) { it.copy(downloadedBytes = w) }
                        }
                    }
                }
                finish(item, part, written)
            }
        } catch (ce: CancellationException) {
            // 用户暂停：字节已经落盘，状态由 pause() 置为 PAUSED。
            throw ce
        } catch (t: Throwable) {
            if (item.id in paused) {
                update(item.id) { it.copy(status = DownloadStatus.PAUSED) }
            } else {
                update(item.id) {
                    it.copy(status = DownloadStatus.FAILED, downloadedBytes = part.length())
                }
            }
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun finish(item: DownloadItem, part: File, bytes: Long) {
        val target = uniqueName(item.fileName, item.id)
        val ok = runCatching { part.renameTo(File(dir, target)) }.getOrDefault(false)
        if (!ok) {
            // 重命名失败（跨分区等）就复制一份再删。
            runCatching {
                val dest = File(dir, target)
                part.inputStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                part.delete()
            }
        }
        val privateCopy = File(dir, target)
        var name = target
        var localUri = Uri.fromFile(privateCopy).toString()

        // 转存到系统公共 Download 目录：私有目录里的成品在系统文件管理器里根本看不到。
        // 转存失败就保留私有副本——已经下好的文件不能因为"搬不动"就丢掉。
        val published = runCatching { PublicDownloads.publish(context, privateCopy) }.getOrNull()
        if (published != null) {
            name = published.displayName
            localUri = published.uri.toString()
            runCatching { privateCopy.delete() }
        }

        update(item.id) {
            it.copy(
                fileName = name,
                status = DownloadStatus.SUCCESS,
                downloadedBytes = if (bytes > 0) bytes else privateCopy.length(),
                totalBytes = if (it.totalBytes > 0) it.totalBytes else privateCopy.length(),
                localUri = localUri,
            )
        }
        jobs.remove(item.id)
        meta.remove(item.id)
    }

    /** 成品是否还在：转存到公共目录后 `localUri` 是 content://，不能再拿私有目录判存在。 */
    private fun existsOnDisk(item: DownloadItem): Boolean {
        val uri = item.localUri
        if (uri != null && uri.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
            }.getOrDefault(false)
        }
        return File(dir, item.fileName).exists()
    }

    private fun update(id: Long, block: (DownloadItem) -> DownloadItem) {
        var next: DownloadItem? = null
        var statusChanged = false
        _items.value = _items.value.map {
            if (it.id == id) {
                val updated = block(it)
                next = updated
                statusChanged = updated.status != it.status
                updated
            } else {
                it
            }
        }
        next?.let { persist(it, force = statusChanged) }
    }

    private fun partFile(item: DownloadItem): File = File(dir, "${item.id}-${item.fileName}.part")

    /**
     * 推断落盘文件名。
     *
     * 不能直接交给 `URLUtil.guessFileName`：它会拿 MIME 推断出的扩展名**覆盖** URL 里的扩展名。
     * `ls-lR.gz` 的 Content-Type 是 `application/octet-stream`，映射到 `bin`，
     * 于是文件被存成 `ls-lR.bin`——用户下完双击是打不开的。
     *
     * 优先级：Content-Disposition 明确指定的名字 > URL 自带的扩展名 > 系统推断。
     */
    private fun guessName(url: String, contentDisposition: String?, mimeType: String?): String {
        dispositionName(contentDisposition)?.let { return it }
        val segment = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
            ?.substringBefore('?')
            ?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        if (!segment.isNullOrBlank()) {
            val ext = segment.substringAfterLast('.', "")
            // 扩展名像样（字母数字、不超过 8 位）才采信，否则「1250」这种也会被当成后缀。
            if (ext.length in 1..8 && ext.all { it.isLetterOrDigit() }) return segment
        }
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    private fun dispositionName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        // RFC 5987 的 filename*= UTF-8''xxx 支持非 ASCII 文件名，优先。
        Regex("""filename\*\s*=\s*[^']*'[^']*'([^;]+)""", RegexOption.IGNORE_CASE)
            .find(header)?.groupValues?.getOrNull(1)?.trim()?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?.let { return runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        return Regex(
            """filename\s*=\s*"([^"]+)"|filename\s*=\s*([^;]+)""",
            RegexOption.IGNORE_CASE,
        ).find(header)?.let { match ->
            match.groupValues[1].ifBlank { match.groupValues[2] }.trim().ifBlank { null }
        }
    }

    /**
     * 取一个不冲突的文件名。
     *
     * 除了看磁盘上是否已有同名成品，还要看**队列里**是否已有同名任务：
     * 两个下载同时开始时会各自看到"磁盘上没有"，从而拿到同一个名字，
     * 表现在界面上就是两条一模一样的记录。
     */
    private fun uniqueName(name: String, selfId: Long? = null): String {
        val base = name.ifBlank { "download.bin" }
        if (isFree(base, selfId)) return base
        val dot = base.lastIndexOf('.')
        val stem = if (dot > 0) base.substring(0, dot) else base
        val ext = if (dot > 0) base.substring(dot) else ""
        var i = 1
        while (true) {
            val candidate = "$stem($i)$ext"
            if (isFree(candidate, selfId)) return candidate
            i++
        }
    }

    private fun isFree(candidate: String, selfId: Long?): Boolean {
        if (File(dir, candidate).exists()) return false
        // 成品下载完成会被转存到公共 Download 目录，那边有同名文件同样要避让，
        // 否则 MediaStore 会默默改名成 "xxx (1).zip"，界面上会出现两条看起来一样的记录。
        if (PublicDownloads.exists(context, candidate)) return false
        if (_items.value.any { it.id != selfId && it.fileName == candidate }) return false
        val suffix = "-$candidate.part"
        val busy = dir.listFiles { f -> f.name.endsWith(suffix) }?.any {
            selfId == null || !it.name.startsWith("$selfId-")
        } ?: false
        return !busy
    }
}
