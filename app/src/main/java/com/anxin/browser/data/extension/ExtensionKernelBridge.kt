package com.anxin.browser.data.extension

/**
 * 用户扩展（第一阶段为「类扩展」：本地脚本包，非 Chrome CRX）。
 * 内核侧真实 Chrome 扩展接入后，扩展中心 UI 与本模型桥接层不变。
 */
data class UserExtension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val script: String,
    val matches: List<String>,
    val runAt: String = "document_end",
    val enabled: Boolean = true,
    val pinned: Boolean = false,
    val sourceName: String = "",
    val installedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MAX_SCRIPT_CHARS = 200_000
        const val MAX_COUNT = 20
    }
}

/**
 * 扩展内核桥（对齐小米浏览器 ExtensionKernelBridge 的分层思路）。
 *
 * - 第一阶段实现：[ScriptExtensionBridge]（类扩展：zip manifest 包 / .user.js 单文件）；
 * - 内核侧 Chromium 扩展（HyperWeb.loadExtension 同类能力）验证通过后，
 *   新增 ChromiumExtensionBridge 实现本接口即可替换，扩展中心 UI 零改动。
 */
interface ExtensionKernelBridge {
    /** 已安装扩展（UI 观察）。 */
    fun observe(): kotlinx.coroutines.flow.StateFlow<List<UserExtension>>

    /** 从本地文件导入（zip: manifest.json + 脚本；或 .user.js 单文件）。 */
    suspend fun importFromFile(path: String, sourceName: String): UserExtension?

    suspend fun remove(id: String)

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun setPinned(id: String, pinned: Boolean)
}
