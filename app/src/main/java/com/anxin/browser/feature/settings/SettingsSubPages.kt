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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.anxin.browser.R
import com.anxin.browser.core.util.Urls
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.anxinDivider
import com.anxin.browser.design.anxinField
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPrimaryButton
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
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
    onModeChange: (String) -> Unit,
    onCustomHomepageChange: (String) -> Unit,
    onWallpaperChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val palette = AnxinPalette
    SubPageScaffold(title = "主页设置", onBack = onBack) {
        SettingsGroup(title = "主页内容") {
            ModeRow("默认主页", "品牌标识 + 搜索 + 快捷入口", mode == "default") { onModeChange("default") }
            divider()
            ModeRow("自定义主页", "每次打开主页直达指定网址", mode == "custom") { onModeChange("custom") }
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
    onCheckUpdate: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
) {
    val palette = AnxinPalette
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
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCheckUpdate)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "检查更新",
                style = MaterialTheme.typography.titleSmall,
                color = palette.brand,
            )
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
