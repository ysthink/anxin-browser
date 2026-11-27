package com.anxin.browser.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anxin.browser.R
import com.anxin.browser.core.util.Urls
import com.anxin.browser.data.update.UpdateCenter
import com.anxin.browser.data.update.UpdateUiState
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinDivider
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPrimaryButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.BrandMark
import com.anxin.browser.ui.NavActions
import com.anxin.browser.ui.PageTitle

// ---------------------------------------------------------------------------
// 公共骨架
// ---------------------------------------------------------------------------

@Composable
private fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = AnxinPalette
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.Rounded.ArrowBack, "返回", size = 36.dp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                PageTitle(title = title)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                content()
                Spacer(Modifier.height(120.dp))
            }
        }
        AnxinActionBar(
            actions = NavActions(onBack, {}, {}, {}, {}),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = anxinDivider(),
            ),
        )
    }
}

@Composable
internal fun SettingsGroup(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val palette = AnxinPalette
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = palette.inkSecondary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 22.dp, alpha = 0.6f, shadowElevation = 12.dp),
            content = content,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(start = 14.dp, end = 14.dp)
            .fillMaxWidth()
            .height(0.7.dp)
            .background(anxinDivider()),
    )
}

// ---------------------------------------------------------------------------
// 主页设置
// ---------------------------------------------------------------------------

/** 壁纸选项：index -> (名称, 资源 id, 预览渐变)。 */
object WallpaperOptions {
    val entries: List<Triple<Int, String, Int>> = listOf(
        Triple(0, "极光蓝 · 默认", R.drawable.hero_wallpaper),
        Triple(1, "晨曦橙", R.drawable.video_poster),
    )
}

@Composable
fun HomeSettingsScreen(
    mode: String,
    customHomepage: String,
    wallpaperIndex: Int,
    showShortcuts: Boolean,
    onModeChange: (String) -> Unit,
    onCustomHomepageChange: (String) -> Unit,
    onWallpaperChange: (Int) -> Unit,
    onShowShortcutsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val palette = AnxinPalette
    SubPageScaffold(title = "主页设置", onBack = onBack) {
        SettingsGroup(title = "主页内容") {
            ModeRow("默认主页", "品牌标识 + 搜索 + 快捷入口", mode == "default") { onModeChange("default") }
            divider()
            ModeRow("自定义主页", "每次打开主页直达指定网址", mode == "custom") { onModeChange("custom") }
        }

        if (mode == "default") {
            SettingsGroup(title = "主页模块") {
                SettingsSwitchRow(
                    title = "显示常用网站",
                    subtitle = "隐藏后主页只保留搜索与最近使用",
                    checked = showShortcuts,
                    onChange = onShowShortcutsChange,
                )
            }
        }

        if (mode == "custom") {
            SettingsGroup(title = "自定义主页地址") {
                Box(Modifier.fillMaxWidth().padding(14.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = customHomepage,
                        onValueChange = onCustomHomepageChange,
                        singleLine = true,
                        placeholder = { Text("https://example.com", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SettingsGroup(title = "设置壁纸") {
            WallpaperOptions.entries.forEachIndexed { index, (_, name, res) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWallpaperChange(index) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF93B5FF)),
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = MaterialTheme.typography.bodyMedium, color = palette.ink, modifier = Modifier.weight(1f))
                    if (wallpaperIndex == index) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "已选择",
                            tint = palette.brand,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                if (index != WallpaperOptions.entries.lastIndex) divider()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWallpaperChange(2) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFF3D6BFF), Color(0xFF57C7FF))),
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Text("紫蓝渐变", style = MaterialTheme.typography.bodyMedium, color = palette.ink, modifier = Modifier.weight(1f))
                if (wallpaperIndex == 2) {
                    Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = palette.brand, modifier = Modifier.size(20.dp))
                }
            }
        }

        Text(
            "壁纸仅作用于应用内首页显示，不影响网页内容。",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkTertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun ModeRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) palette.brand else anxinField()),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 搜索设置
// ---------------------------------------------------------------------------

@Composable
fun SearchSettingsScreen(
    searchEngine: String,
    showEngineBar: Boolean,
    onOpenSearchEngine: () -> Unit,
    onEngineBarChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    SubPageScaffold(title = "搜索设置", onBack = onBack) {
        SettingsGroup(title = "搜索引擎") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSearchEngine)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Language,
                    contentDescription = null,
                    tint = AnxinPalette.brand,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text("设置搜索引擎", style = MaterialTheme.typography.titleSmall, color = AnxinPalette.ink, modifier = Modifier.weight(1f))
                Text(
                    searchEngine,
                    style = MaterialTheme.typography.labelMedium,
                    color = AnxinPalette.brandDeep,
                )
            }
            divider()
            SettingsSwitchRow(
                title = "显示搜索引擎切换栏",
                subtitle = "首页与网页浏览态顶部展示切换入口",
                checked = showEngineBar,
                onChange = onEngineBarChange,
            )
        }
        Text(
            "当前支持的引擎：必应（默认）、百度、谷歌。",
            style = MaterialTheme.typography.labelSmall,
            color = AnxinPalette.inkTertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 网页浏览设置
// ---------------------------------------------------------------------------

@Composable
fun WebSettingsScreen(
    textZoom: Int,
    desktopMode: Boolean,
    onTextZoomChange: (Int) -> Unit,
    onDesktopModeChange: (Boolean) -> Unit,
    onResetWebSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = AnxinPalette
    SubPageScaffold(title = "网页浏览设置", onBack = onBack) {
        SettingsGroup(title = "显示与阅读") {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("文字大小", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "$textZoom%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.brandDeep,
                )
                Slider(
                    value = textZoom.toFloat(),
                    onValueChange = { onTextZoomChange(it.toInt()) },
                    valueRange = 70f..150f,
                    steps = 15,
                )
                Text(
                    "调整网页内文字显示大小，立即对所有标签生效。",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
            }
        }

        SettingsGroup(title = "访问方式") {
            SettingsSwitchRow(
                title = "访问电脑版",
                subtitle = "以桌面 UA 请求网页，切换后自动刷新",
                checked = desktopMode,
                onChange = onDesktopModeChange,
            )
        }

        GlassPrimaryButton(
            text = "恢复默认显示设置",
            modifier = Modifier.fillMaxWidth(),
            onClick = onResetWebSettings,
        )
    }
}

// ---------------------------------------------------------------------------
// 播放设置
// ---------------------------------------------------------------------------

@Composable
fun PlaySettingsScreen(
    playDynamicVideo: Boolean,
    backgroundPlay: Boolean,
    autoMiniWindow: Boolean,
    pipEnabled: Boolean,
    onPlayDynamicVideoChange: (Boolean) -> Unit,
    onBackgroundPlayChange: (Boolean) -> Unit,
    onAutoMiniWindowChange: (Boolean) -> Unit,
    onPipChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    SubPageScaffold(title = "播放设置", onBack = onBack) {
        SettingsGroup(title = "视频播放") {
            SettingsSwitchRow(
                title = "动态内容视频模式播放",
                subtitle = "对 GIF 等动态内容按视频方式解码（WebView 阶段为开关占位）",
                checked = playDynamicVideo,
                onChange = onPlayDynamicVideoChange,
            )
            divider()
            SettingsSwitchRow(
                title = "后台播放",
                subtitle = "切到后台继续播放音频",
                checked = backgroundPlay,
                onChange = onBackgroundPlayChange,
            )
            divider()
            SettingsSwitchRow(
                title = "自动小窗",
                subtitle = "离开网页时自动进入画中画",
                checked = autoMiniWindow,
                onChange = onAutoMiniWindowChange,
            )
            divider()
            SettingsSwitchRow(
                title = "画中画按钮",
                subtitle = "播放器内显示画中画切换",
                checked = pipEnabled,
                onChange = onPipChange,
            )
        }
        Text(
            "画中画与后台播放需要播放器页面在前台使用，网页内嵌视频由内核决定。",
            style = MaterialTheme.typography.labelSmall,
            color = AnxinPalette.inkTertiary,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 关于
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
) {
    val palette = AnxinPalette
    val context = LocalContext.current
    val updateState by UpdateCenter.state.collectAsStateWithLifecycle()

    // 下载完成 → 拉起系统安装器（只自动触发一次，回来后点行可再拉起）
    var installTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(updateState) {
        val s = updateState
        if (s is UpdateUiState.ReadyToInstall && !installTriggered) {
            installTriggered = true
            runCatching {
                context.startActivity(UpdateCenter.buildInstallIntent(context, s.apk))
            }
        }
    }

    val webViewVersion = remember {
        runCatching { android.webkit.WebView.getCurrentWebViewPackage()?.versionName }.getOrNull()
    }
    val kernelLabel = if (com.anxin.browser.BuildConfig.USE_CHROMIUM_KERNEL) {
        "自研 Chromium 内核"
    } else {
        "系统 WebView" + (webViewVersion?.let { " $it" } ?: "")
    }

    SubPageScaffold(title = "关于", onBack = onBack) {
        // 品牌区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 24.dp, alpha = 0.62f, shadowElevation = 16.dp)
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 60, corner = 18)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("安心浏览器", style = MaterialTheme.typography.titleLarge, color = palette.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "v${com.anxin.browser.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "更安全 · 更流畅 · 更好用",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkTertiary,
                )
            }
        }

        SettingsGroup("版本信息") {
            AboutRow("版本号", "v${com.anxin.browser.BuildConfig.VERSION_NAME}（${com.anxin.browser.BuildConfig.VERSION_CODE}）")
            divider()
            AboutRow("渲染内核", kernelLabel)
            if (com.anxin.browser.BuildConfig.USE_CHROMIUM_KERNEL) {
                divider()
                AboutRow("内核架构", com.anxin.browser.BuildConfig.KERNEL_ABI)
            }
            divider()
            AboutRow("构建类型", com.anxin.browser.BuildConfig.BUILD_TYPE)
        }

        SettingsGroup("隐私与数据") {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    "书签、历史、下载记录均保存在本机；无痕标签关闭后不落盘。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "应用不收集浏览内容，不含第三方统计 SDK。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary,
                )
            }
            divider()
            AboutActionRow("隐私说明") { onPrivacy() }
        }

        SettingsGroup("开源许可") {
            AboutActionRow("开源组件与许可") { onLicenses() }
        }

        // 检查更新入口：行内实时反映状态
        val (updateLabel, updateBusy) = when (val s = updateState) {
            is UpdateUiState.Checking -> "检查中…" to true
            is UpdateUiState.Downloading -> "下载中 ${(s.received * 100 / s.total.coerceAtLeast(1))}%" to true
            is UpdateUiState.UpToDate -> "已是最新版本" to false
            is UpdateUiState.Available -> "发现新版本 v${s.info.versionName}" to false
            else -> "检查更新" to false
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !updateBusy) {
                    when (val st = updateState) {
                        is UpdateUiState.ReadyToInstall -> {
                            installTriggered = false
                            runCatching {
                                context.startActivity(UpdateCenter.buildInstallIntent(context, st.apk))
                            }
                        }
                        is UpdateUiState.Available -> Unit // 走弹窗
                        else -> UpdateCenter.check()
                    }
                }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                updateLabel,
                style = MaterialTheme.typography.titleSmall,
                color = if (updateState is UpdateUiState.Available) {
                    MaterialTheme.colorScheme.error
                } else {
                    palette.brand
                },
            )
        }

        // ---- 更新弹窗 ----
        when (val s = updateState) {
            is UpdateUiState.Available -> {
                AlertDialog(
                    onDismissRequest = { if (!s.info.forceUpdate) UpdateCenter.reset() },
                    title = { Text("发现新版本 v${s.info.versionName}") },
                    text = {
                        Text(
                            s.info.notes.ifBlank {
                                "新版本 v${s.info.versionName}（${s.info.versionCode}）可用，是否更新？"
                            },
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { UpdateCenter.startDownload(context) }) {
                            Text("立即更新", color = palette.brand)
                        }
                    },
                    dismissButton = {
                        if (!s.info.forceUpdate) {
                            TextButton(onClick = { UpdateCenter.reset() }) { Text("稍后") }
                        }
                    },
                )
            }
            is UpdateUiState.Downloading -> {
                val percent = if (s.total > 0) (s.received * 100 / s.total).toInt() else 0
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("正在下载更新") },
                    text = {
                        Column {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$percent%（${s.received / 1024 / 1024}MB / ${s.total / 1024 / 1024}MB）",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkSecondary,
                            )
                        }
                    },
                    confirmButton = {},
                )
            }
            is UpdateUiState.Failed -> {
                AlertDialog(
                    onDismissRequest = { UpdateCenter.reset() },
                    title = { Text("更新失败") },
                    text = { Text(s.message) },
                    confirmButton = {
                        TextButton(onClick = { UpdateCenter.check() }) { Text("重试") }
                    },
                    dismissButton = {
                        TextButton(onClick = { UpdateCenter.reset() }) { Text("取消") }
                    },
                )
            }
            else -> Unit
        }

        Text(
            "© 2026 安心浏览器 · 保留所有权利",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = palette.inkSecondary)
    }
}

@Composable
private fun AboutActionRow(label: String, onClick: () -> Unit) {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleMedium, color = palette.inkTertiary)
    }
}

// ---------------------------------------------------------------------------
// 隐私说明
// ---------------------------------------------------------------------------

/**
 * 隐私说明页。
 *
 * 这里刻意写成"可核对的具体条目"而不是泛泛的免责声明：每一条都要和代码里的
 * 实际行为对得上（存储位置、权限用途、第三方组件），否则就是自欺欺人。
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val palette = AnxinPalette

    SubPageScaffold(title = "隐私说明", onBack = onBack) {
        // 承诺摘要
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 22.dp, alpha = 0.62f, shadowElevation = 14.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text("一句话概括", style = MaterialTheme.typography.labelMedium, color = palette.brand)
            Spacer(Modifier.height(6.dp))
            Text(
                "安心浏览器不收集、不上传你的浏览记录。所有个人数据只存在这台设备上，卸载即清除。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
            )
        }

        LegalSection(
            title = "我们不会做的事",
            items = listOf(
                "不收集你的浏览历史、搜索词、书签或下载记录；",
                "不申请通讯录、短信、位置、相机、麦克风等与浏览无关的权限；",
                "不集成第三方统计、广告、推送或崩溃上报 SDK；",
                "不含任何形式的账号体系，无需注册即可使用全部功能。",
            ),
        )

        SettingsGroup("保存在本机的数据") {
            DataRow("书签", "标题、网址、创建时间")
            divider()
            DataRow("历史记录", "标题、网址、访问时间（可随时一键清空）")
            divider()
            DataRow("下载记录", "文件名、来源网址、进度")
            divider()
            DataRow("扩展与脚本", "你手动导入的扩展包与用户脚本内容")
            divider()
            DataRow("偏好设置", "主页、搜索引擎、夜间模式、菜单布局等")
            divider()
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    "以上数据存放在应用私有目录（Android/data/com.anxin.browser），其他应用无法读取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary,
                )
            }
        }

        LegalSection(
            title = "无痕模式",
            items = listOf(
                "无痕标签的浏览数据不写入本地数据库，也不进入历史记录；",
                "关闭无痕标签后，该次会话的 Cookie 与缓存一并丢弃；",
                "下载的文件本身仍会保留在下载目录——这是文件系统的行为，不是我们在记录。",
            ),
        )

        SettingsGroup("权限与用途") {
            DataRow("网络访问", "加载网页，唯一必需的权限")
            divider()
            DataRow("网络状态", "断网时给出提示，避免空白页")
            divider()
            DataRow("通知", "下载完成、媒体后台播放的进度提示")
            divider()
            DataRow("前台服务", "退到后台后继续播放视频或完成下载")
            divider()
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    "以上权限均可在系统设置中随时关闭，关闭后对应功能停用，不影响其他部分。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary,
                )
            }
        }

        LegalSection(
            title = "网页与第三方",
            items = listOf(
                "你访问的网站由网站方自行决定收集哪些信息，其隐私政策与本应用无关；",
                "下载由应用内置的下载器完成（支持断点续传），文件保存在应用私有目录；",
                "阅读模式、网页翻译等本地处理功能在你设备上完成，不经过我们的服务器——事实上我们没有服务器。",
            ),
        )

        SettingsGroup("你的控制权") {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    "历史记录页可一键清空全部记录；书签、下载记录支持逐条删除；设置页底部提供「恢复默认设置」，会重置全部偏好。",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSecondary,
                )
            }
        }

        Text(
            "本说明适用于 安心浏览器 v${com.anxin.browser.BuildConfig.VERSION_NAME}\n最近更新：2026 年 1 月",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun LegalSection(title: String, items: List<String>) {
    val palette = AnxinPalette
    SettingsGroup(title) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                Row {
                    Text("·", style = MaterialTheme.typography.bodySmall, color = palette.brand)
                    Spacer(Modifier.width(6.dp))
                    Text(item, style = MaterialTheme.typography.bodySmall, color = palette.inkSecondary)
                }
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    val palette = AnxinPalette
    Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = palette.ink)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
    }
}

// ---------------------------------------------------------------------------
// 开源许可
// ---------------------------------------------------------------------------

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val palette = AnxinPalette
    val kernel = if (com.anxin.browser.BuildConfig.USE_CHROMIUM_KERNEL) {
        "自研 Chromium 内核（${com.anxin.browser.BuildConfig.KERNEL_ABI}）"
    } else {
        "Android System WebView（系统组件）"
    }

    SubPageScaffold(title = "开源许可", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 22.dp, alpha = 0.62f, shadowElevation = 14.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text("本应用得以存在", style = MaterialTheme.typography.labelMedium, color = palette.brand)
            Spacer(Modifier.height(6.dp))
            Text(
                "安心浏览器站在大量开源项目之上。以下列出主要组件及其许可证，完整许可见各项目仓库。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkSecondary,
            )
        }

        SettingsGroup("渲染内核") {
            LicenseRow(kernel, "BSD-3-Clause", "页面渲染、V8 JavaScript 引擎、网络栈")
        }

        SettingsGroup("界面与运行时") {
            LicenseRow("AndroidX / Jetpack Compose", "Apache-2.0", "声明式 UI、导航、生命周期")
            divider()
            LicenseRow("Material Icons", "Apache-2.0", "界面图标")
            divider()
            LicenseRow("Kotlin & Kotlin Coroutines", "Apache-2.0", "开发语言与协程")
        }

        SettingsGroup("数据与媒体") {
            LicenseRow("Room", "Apache-2.0", "书签、历史、下载记录的本地数据库")
            divider()
            LicenseRow("DataStore", "Apache-2.0", "偏好设置的键值存储")
            divider()
            LicenseRow("Media3 / ExoPlayer", "Apache-2.0", "媒体嗅探后的本地播放")
            divider()
            LicenseRow("Coil", "Apache-2.0", "图标与缩略图加载")
        }

        SettingsGroup("内核桥接") {
            LicenseRow("jni_zero", "BSD-3-Clause", "Java 与 Native 的 JNI 绑定生成")
        }

        SettingsGroup("字体") {
            LicenseRow("系统默认字体", "随系统提供", "未内置第三方字体，避免额外授权与体积")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassPanel(radius = 20.dp, alpha = 0.58f, shadowElevation = 10.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "内核版包含 Chromium 完整许可清单。如需逐条查阅，请在地址栏访问 chrome://credits（仅内核版可用）。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkSecondary,
            )
        }

        Text(
            "© 2026 安心浏览器 · 各组件版权归其作者所有",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun LicenseRow(name: String, license: String, usage: String) {
    val palette = AnxinPalette
    Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = palette.ink, modifier = Modifier.weight(1f))
            Text(
                license,
                style = MaterialTheme.typography.labelSmall,
                color = palette.brand,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.brand.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(usage, style = MaterialTheme.typography.labelSmall, color = palette.inkTertiary)
    }
}
