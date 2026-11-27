package com.anxin.browser.feature.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anxin.browser.data.extension.UserExtension
import com.anxin.browser.design.AnxinDialogContainer
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassSegments
import com.anxin.browser.design.anxinDivider
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions

/**
 * 扩展探索目录：精选 / 热门两档，全部为可公开获取的真实扩展。
 *
 * 本阶段内核走「脚本包」注入，不能直接安装 CRX，所以「安装/获取」
 * 的语义是：打开官方获取页面 → 用户下载 .user.js / zip → 回扩展中心
 * 「导入本地扩展文件」完成安装。目录只做发现与引导，不伪装安装成功。
 */
data class CatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val detail: String,
    val url: String,
    val featured: Boolean,
    val iconText: String,
    val iconColors: List<Color>,
)

object ExtensionCatalog {
    val entries: List<CatalogEntry> = listOf(
        CatalogEntry(
            id = "immersive-translate",
            name = "沉浸式翻译",
            description = "【沉浸式翻译】一款免费的（原文/译文）AI 双语对照网页翻译插件",
            detail = "双语对照阅读外文网页：段落级原文/译文对照，支持 10+ 翻译引擎。" +
                "获取后下载油猴脚本版（.user.js），在本浏览器「导入本地扩展文件」完成安装。",
            url = "https://immersivetranslate.com/",
            featured = true,
            iconText = "译",
            iconColors = listOf(Color(0xFFB18CFF), Color(0xFF7B4DFF)),
        ),
        CatalogEntry(
            id = "youdao-ling",
            name = "有道灵动翻译",
            description = "使用有道翻译大模型，沉浸式网页翻译的首选工具！实时对照翻译、图片翻译…",
            detail = "有道词典官方网页翻译插件：划词翻译、对照翻译、整页翻译。" +
                "从官网获取脚本版后通过本地导入安装。",
            url = "https://fanyi.youdao.com/",
            featured = true,
            iconText = "Yd",
            iconColors = listOf(Color(0xFFFF8A8A), Color(0xFFE5322D)),
        ),
        CatalogEntry(
            id = "fetchv",
            name = "FetchV",
            description = "这是一个通用的网页视频下载工具，适用于下载网页中的 HLS (HTTP Live…",
            detail = "嗅探网页里的 HLS/MP4 视频资源并合并下载。与浏览器内置「资源嗅探」" +
                "互补：分片合并能力更强。获取页提供脚本版安装说明。",
            url = "https://fetchv.net/",
            featured = false,
            iconText = "▶",
            iconColors = listOf(Color(0xFF59D0FF), Color(0xFF2B6BFF)),
        ),
        CatalogEntry(
            id = "ublock-lite",
            name = "uBlock Origin Lite",
            description = "一款高效的网络请求过滤工具，占用极低的内存和 CPU。",
            detail = "广告与跟踪请求过滤。注意：完整版为 Manifest V3 浏览器扩展，" +
                "当前内核以脚本形式提供轻量规则过滤；浏览器设置中的「广告过滤」已内置静态规则，" +
                "此条目供从商店获取进阶方案。",
            url = "https://chromewebstore.google.com/detail/ublock-origin-lite/ddkjiahejlhfcafbddmgiahcphecmpfh",
            featured = false,
            iconText = "ub",
            iconColors = listOf(Color(0xFF8B2E2E), Color(0xFF5A1414)),
        ),
        CatalogEntry(
            id = "ruffle",
            name = "Ruffle - Flash Emulator",
            description = "将 Flash 重新带回网络。",
            detail = "用 Rust 编写的 Flash Player 模拟器，以脚本注入方式替换页面中的" +
                "Flash 内容。获取页提供脚本版。",
            url = "https://ruffle.rs/",
            featured = false,
            iconText = "Ru",
            iconColors = listOf(Color(0xFFFFB35C), Color(0xFFF07C10)),
        ),
        CatalogEntry(
            id = "tampermonkey",
            name = "Tampermonkey",
            description = "最流行的用户脚本管理器，为数百万用户提供便捷的脚本管理。",
            detail = "用户脚本管理器。本浏览器已内置 GM_* 兼容层并支持 .user.js 直接导入，" +
                "如需脚本市场可在 GreasyFork 查找脚本后本地导入。",
            url = "https://www.tampermonkey.net/",
            featured = false,
            iconText = "TM",
            iconColors = listOf(Color(0xFF4A4A52), Color(0xFF1C1C22)),
        ),
    )

    val featured get() = entries.filter { it.featured }
    val popular get() = entries.filterNot { it.featured }
}

/**
 * 扩展中心（参考图改版）：
 * - 「我的扩展」页签：已安装列表 + 启用/停用/固定/删除；
 * - 「扩展探索」页签：精选扩展 / 热门扩展目录卡，安装按钮跳官方获取页；
 * - 右上 ⋮ 菜单：Edge 商店 / Chrome 商店 / 导入本地扩展文件 / 菜单显示常用扩展开关。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    extensions: List<UserExtension>,
    showDisclaimer: Boolean,
    initialTab: Int,
    showExtInMenu: Boolean,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onPin: (id: String, pinned: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    onDisclaimerAccept: () -> Unit,
    onShowExtInMenuChange: (Boolean) -> Unit,
    onOpenStore: (url: String) -> Unit,
    actions: NavActions,
) {
    val palette = AnxinPalette
    var tab by remember { mutableIntStateOf(initialTab) }
    var pendingDelete by remember { mutableStateOf<UserExtension?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var detailEntry by remember { mutableStateOf<CatalogEntry?>(null) }

    AuroraBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIconButton(Icons.Rounded.ArrowBack, "返回", size = 36.dp, onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "扩展程序",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "本地安装 · 仅本机生效",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.inkTertiary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        GlassIconButton(Icons.Rounded.MoreVert, "更多", size = 36.dp, onClick = { menuOpen = true })
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = AnxinDialogContainer,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("从 Microsoft Edge 扩展获取", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    menuOpen = false
                                    onOpenStore("https://microsoftedge.microsoft.com/addons/Microsoft-Edge-Extensions-Home")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("从 Chrome Web Store 获取", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    menuOpen = false
                                    onOpenStore("https://chromewebstore.google.com/")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("导入本地扩展文件", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    menuOpen = false
                                    onImport()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "菜单显示常用扩展 ${if (showExtInMenu) "✓" else ""}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (showExtInMenu) palette.brand else palette.ink,
                                    )
                                },
                                onClick = { onShowExtInMenuChange(!showExtInMenu) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                GlassSegments(
                    labels = listOf("我的扩展", "扩展探索"),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
                Spacer(Modifier.height(12.dp))

                if (tab == 0) {
                    if (extensions.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(120.dp))
                            Icon(
                                Icons.Rounded.Extension,
                                contentDescription = null,
                                tint = palette.inkTertiary,
                                modifier = Modifier.size(52.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "暂未安装任何扩展",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "前往“扩展探索”获取广告拦截、AI 工具等扩展插件",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkTertiary,
                            )
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.brand.copy(alpha = 0.12f))
                                    .clickable { tab = 1 }
                                    .padding(horizontal = 22.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "去扩展探索",
                                    color = palette.brand,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "也可以点右上 ⋮ 导入本地扩展文件（.user.js 或 zip）",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkTertiary.copy(alpha = 0.85f),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            // 底部留白避开悬浮工具栏，最后一张卡片才能完整滑出来。
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
                        ) {
                            items(extensions, key = { it.id }) { ext ->
                                ExtensionCard(
                                    ext = ext,
                                    onToggle = { onToggle(ext.id, it) },
                                    onPin = { onPin(ext.id, it) },
                                    onDelete = { pendingDelete = ext },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp),
                    ) {
                        item {
                            Text(
                                "精选扩展",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.inkTertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                        }
                        items(ExtensionCatalog.featured, key = { "f-" + it.id }) { entry ->
                            CatalogCard(
                                entry = entry,
                                onGet = { onOpenStore(entry.url) },
                                onDetail = { detailEntry = entry },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        item {
                            Text(
                                "热门扩展",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.inkTertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            )
                        }
                        items(ExtensionCatalog.popular, key = { "p-" + it.id }) { entry ->
                            CatalogCard(
                                entry = entry,
                                onGet = { onOpenStore(entry.url) },
                                onDetail = { detailEntry = entry },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        item {
                            Text(
                                "目录条目点击「安装/获取」会打开官方获取页面；下载 .user.js 或 zip 后，" +
                                    "通过右上菜单「导入本地扩展文件」完成安装。",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkTertiary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
            AnxinActionBar(
                actions = actions,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = AnxinDialogContainer,
            shape = RoundedCornerShape(24.dp),
            title = { Text("使用前须知", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "扩展脚本将在你浏览的网页中运行，可能读取或修改页面内容。\n\n" +
                        "请仅安装可信来源的扩展包；企业场景建议只导入内部签名的脚本。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkSecondary,
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = onDisclaimerAccept,
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AnxinPalette.brand,
                        contentColor = palette.onAccent,
                    ),
                ) { Text("我已知晓", fontWeight = FontWeight.SemiBold) }
            },
        )
    }

    detailEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { detailEntry = null },
            containerColor = AnxinDialogContainer,
            shape = RoundedCornerShape(24.dp),
            title = { Text(entry.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        entry.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        entry.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "获取页面：${entry.url}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.brand,
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        detailEntry = null
                        onOpenStore(entry.url)
                    },
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        contentColor = palette.onAccent,
                    ),
                ) { Text("去获取", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { detailEntry = null }) { Text("关闭") }
            },
        )
    }

    pendingDelete?.let { ext ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除扩展") },
            text = { Text("确定删除「${ext.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(ext.id)
                    pendingDelete = null
                }) { Text("删除", color = palette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

/** 扩展探索目录卡片：图标 + 名称 + 两行描述 + 详细信息 + 安装/获取按钮。 */
@Composable
private fun CatalogCard(
    entry: CatalogEntry,
    onGet: () -> Unit,
    onDetail: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(radius = 20.dp, alpha = 0.55f, shadowElevation = 8.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(entry.iconColors)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.iconText,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                entry.description,
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "详细信息",
                style = MaterialTheme.typography.labelSmall,
                color = palette.brand,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDetail)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.brand)
                .clickable(onClick = onGet)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (entry.featured) "安装" else "获取",
                color = palette.onAccent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 菜单里点「扩展程序」弹出的快捷面板（参考图 4）：
 * 上半部：已安装扩展（空态 + 去扩展探索）；
 * 下半部：管理"我的扩展" / 获取更多扩展 两个入口行。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtQuickSheet(
    extensions: List<UserExtension>,
    onDismiss: () -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onManageMine: () -> Unit,
    onExplore: () -> Unit,
) {
    val palette = AnxinPalette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AnxinDialogContainer,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
        ) {
            Text(
                "扩展程序",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.ink,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.ink.copy(alpha = 0.04f))
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (extensions.isEmpty()) {
                    Icon(
                        Icons.Rounded.Explore,
                        contentDescription = null,
                        tint = palette.brand,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "暂未安装任何扩展",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.brand.copy(alpha = 0.12f))
                            .clickable { onExplore() }
                            .padding(horizontal = 22.dp, vertical = 9.dp),
                    ) {
                        Text(
                            "去扩展探索",
                            color = palette.brand,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    extensions.forEach { ext ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF9FC0FF), palette.brand, palette.violet),
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    ext.name.take(1).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ext.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (ext.enabled) palette.ink else palette.inkTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (ext.enabled) "已启用" else "已停用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.inkTertiary,
                                )
                            }
                            Switch(
                                checked = ext.enabled,
                                onCheckedChange = { onToggle(ext.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.brand,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = anxinDivider(),
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            QuickSheetRow(Icons.Rounded.Extension, "管理“我的扩展”", onManageMine)
            QuickSheetRow(Icons.Rounded.Explore, "获取更多扩展", onExplore)
        }
    }
}

@Composable
private fun QuickSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.ink.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = palette.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.ink)
    }
}

/** 「我的扩展」卡片：首字母图标 + 名称/版本/描述/匹配 + 固定/删除/开关。 */
@Composable
private fun ExtensionCard(
    ext: UserExtension,
    onToggle: (Boolean) -> Unit,
    onPin: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(
                radius = 20.dp,
                alpha = if (ext.enabled) 0.6f else 0.4f,
                shadowElevation = 10.dp,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF9FC0FF),
                            if (ext.enabled) palette.brand else Color(0xFF9AA8BE),
                            palette.violet,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ext.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ext.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (ext.enabled) palette.ink else palette.inkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "v${ext.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
                if (ext.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "已固定",
                        tint = palette.brand,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            if (ext.description.isNotBlank()) {
                Text(
                    ext.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "匹配：${ext.matches.joinToString().take(46)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkTertiary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onPin(!ext.pinned) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PushPin,
                contentDescription = if (ext.pinned) "取消固定" else "固定",
                tint = if (ext.pinned) palette.brand else palette.inkTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "删除扩展",
                tint = palette.inkTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(2.dp))
        Switch(
            checked = ext.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = anxinDivider(),
            ),
        )
    }
}
