package com.anxin.browser.core.model

import android.view.View

/** 媒体资源类型。 */
enum class MediaType {
    VIDEO, AUDIO, HLS, DASH, IMAGE, FILE
}

/**
 * 统一媒体资源模型（对应设计方案中的 MediaSniffer 资源仓库条目）。
 * 凭据类信息（Cookie / 鉴权头）不落库，只允许通过受控引用传递。
 */
data class MediaResource(
    val url: String,
    val type: MediaType,
    val mimeType: String? = null,
    val title: String? = null,
    val referer: String? = null,
    val size: Long? = null,
    val duration: Long? = null,
    val sourceTabId: String = "",
)

/**
 * 一个浏览器标签页。
 * [webView] 为内核层视图引用，上层 UI 只通过 [BrowserEngine] 操作它。
 */
data class BrowserTab(
    val id: String,
    val url: String = "about:blank",
    val title: String = "新标签页",
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val faviconUrl: String? = null,
    val webView: View? = null,
    val lastActiveAt: Long = 0L,
)

enum class DownloadStatus { PENDING, RUNNING, PAUSED, SUCCESS, FAILED }

/** 会话恢复单元：一个待恢复标签的最小信息。 */
data class TabSession(
    val url: String,
    val incognito: Boolean = false,
)

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.RUNNING,
    val localUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val fromIncognito: Boolean = false,
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) else 0f

    val speedText: String
        get() = when (status) {
            DownloadStatus.RUNNING -> "下载中"
            DownloadStatus.PAUSED -> "已暂停"
            DownloadStatus.SUCCESS -> "已完成"
            DownloadStatus.FAILED -> "失败"
            DownloadStatus.PENDING -> "等待中"
        }
}

/** 扩展信息（第一阶段仅做展示与本地导入，运行能力依赖内核底座）。 */
data class ExtensionInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val version: String,
    val iconKey: String,
    val installed: Boolean = false,
    val enabled: Boolean = false,
    val isLocal: Boolean = false,
    val manifestVersion: Int = 3,
)
