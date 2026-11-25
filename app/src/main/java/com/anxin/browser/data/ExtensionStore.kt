package com.anxin.browser.data

import com.anxin.browser.core.model.ExtensionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 扩展中心数据源（第一阶段：管理入口 + 状态维护）。
 *
 * 运行扩展需要 Chromium 的 Extension Runtime，WebView 无法承载。
 * 接入 Titanium 底座后，本仓库的 install/enable 行为改为调用
 * Titanium Extension 的安装接口即可，UI 层无需变更。
 */
class ExtensionStore {

    private val _extensions = MutableStateFlow(
        listOf(
            ExtensionInfo(
                id = "ublock-origin",
                name = "广告过滤",
                description = "净化网页，畅享纯净体验",
                category = "推荐",
                version = "1.58.0",
                iconKey = "shield",
                manifestVersion = 2,
                installed = true,
                enabled = true,
            ),
            ExtensionInfo(
                id = "translate",
                name = "翻译助手",
                description = "一键翻译，支持多语言",
                category = "工具",
                version = "2.3.1",
                iconKey = "translate",
            ),
            ExtensionInfo(
                id = "screenshot",
                name = "网页截图",
                description = "快速截图，保存分享",
                category = "工具",
                version = "1.4.2",
                iconKey = "camera",
            ),
            ExtensionInfo(
                id = "tampermonkey",
                name = "Tampermonkey",
                description = "脚本管理器，油猴脚本全兼容",
                category = "效率",
                version = "5.1.1",
                iconKey = "script",
                manifestVersion = 3,
            ),
            ExtensionInfo(
                id = "dark-reader",
                name = "Dark Reader",
                description = "网页夜间模式，护眼深色渲染",
                category = "效率",
                version = "4.9.9",
                iconKey = "moon",
                manifestVersion = 3,
            ),
            ExtensionInfo(
                id = "video-helper",
                name = "视频增强助手",
                description = "倍速、画质、弹幕一站式增强",
                category = "娱乐",
                version = "0.9.6",
                iconKey = "video",
                manifestVersion = 2,
            ),
        )
    )
    val extensions: StateFlow<List<ExtensionInfo>> = _extensions.asStateFlow()

    fun install(id: String) = update(id) { it.copy(installed = true, enabled = true) }

    fun uninstall(id: String) = update(id) { it.copy(installed = false, enabled = false) }

    fun setEnabled(id: String, enabled: Boolean) = update(id) { it.copy(enabled = enabled) }

    fun addLocal(name: String): ExtensionInfo {
        val item = ExtensionInfo(
            id = "local-${System.currentTimeMillis()}",
            name = name,
            description = "本地导入的扩展（等待内核层接管）",
            category = "工具",
            version = "local",
            iconKey = "folder",
            installed = true,
            enabled = true,
            isLocal = true,
        )
        _extensions.value = _extensions.value + item
        return item
    }

    private fun update(id: String, block: (ExtensionInfo) -> ExtensionInfo) {
        _extensions.value = _extensions.value.map { if (it.id == id) block(it) else it }
    }
}
