package com.anxin.browser.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.R
import com.anxin.browser.data.HistoryEntry
import com.anxin.browser.data.Shortcut
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.imeTap
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassPanel
import com.anxin.browser.design.glassPanel
import com.anxin.browser.design.isAnxinDark
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.BrandMark
import com.anxin.browser.ui.NavActions

/**
 * 极简首页：品牌标识 + 搜索栏 + 最近使用 + 用户自己添加的快捷网站。
 * 不展示天气、新闻、推荐资讯或任何热门内容流。
 */
@Composable
fun HomeScreen(
    shortcuts: List<Shortcut>,
    recentSites: List<HistoryEntry>,
    showShortcuts: Boolean,
    actions: NavActions,
    tabCount: Int,
    wallpaperIndex: Int,
    onSearch: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddShortcut: () -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onOpenHomeSettings: () -> Unit,
    searchEngine: String,
    onPickEngine: () -> Unit,
) {
    val palette = AnxinPalette

    AuroraBackground {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val heroHeight = maxHeight * 0.82f
            if (wallpaperIndex != 2) {
                Image(
                    painter = painterResource(
                        if (wallpaperIndex == 1) R.drawable.video_poster else R.drawable.hero_wallpaper
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF57C7FF), Color(0xFF7B5CFF), Color(0xFF3D6BFF)),
                            )
                        ),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (isAnxinDark) {
                            Brush.verticalGradient(
                                0f to Color(0xFF0B0A1F).copy(alpha = 0.74f),
                                0.28f to Color(0xFF171034).copy(alpha = 0.52f),
                                0.52f to palette.pageMid.copy(alpha = 0.90f),
                                0.74f to palette.pageMid,
                                1f to palette.pageBottom,
                            )
                        } else {
                            Brush.verticalGradient(
                                // 浅色壁纸顶部是偏亮的天光，白色主视觉文字需要略厚的蓝调遮罩托底，
                                // 否则小字号副标题对比度不足 4.5:1。
                                0f to Color(0xFF2146B8).copy(alpha = 0.44f),
                                0.22f to Color(0xFF3D6BFF).copy(alpha = 0.18f),
                                0.44f to Color.Transparent,
                                0.58f to palette.pageMid.copy(alpha = 0.86f),
                                0.72f to palette.pageMid,
                                1f to palette.pageBottom,
                            )
                        }
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // 右上角：主页设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp, top = 2.dp)
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .clickable(onClick = onOpenHomeSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Wallpaper,
                            contentDescription = "主页设置",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(34.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrandMark(size = 74, corner = 23, glass = true)
                    Spacer(Modifier.height(14.dp))
                    // 壁纸亮度不可控（浅色天光 / 用户换的橙色壁纸），白色文字加一层柔和阴影，
                    // 比继续压暗遮罩更保画面通透感。
                    Text(
                        "安心浏览器",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.28f),
                                offset = Offset(0f, 2f),
                                blurRadius = 14f,
                            ),
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "更安全 · 更流畅 · 更好用",
                        style = MaterialTheme.typography.labelMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.24f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 10f,
                            ),
                        ),
                        color = Color.White.copy(alpha = 0.94f),
                    )
                }

                Spacer(Modifier.height(40.dp))

                SearchBar(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onSearch = onSearch,
                    searchEngine = searchEngine,
                    onPickEngine = onPickEngine,
                )

                Spacer(Modifier.height(28.dp))

                // 最近使用：每个域名只留最近一次访问，最多一行。空历史不显示。
                if (recentSites.isNotEmpty()) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        radius = 24.dp,
                        alpha = 0.68f,
                        shadowElevation = 18.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 16.dp,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "最近使用",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink,
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                recentSites.take(4).forEachIndexed { index, site ->
                                    RecentSiteItem(
                                        title = site.title,
                                        url = site.url,
                                        color = recentPalette[index % recentPalette.size],
                                        onClick = { onOpenUrl(site.url) },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // 常用网站：用户添加的快捷入口，多行网格，超过两行面板内滚动。可在主页设置中隐藏。
                if (showShortcuts) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        radius = 24.dp,
                        alpha = 0.68f,
                        shadowElevation = 18.dp,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 16.dp,
                        ),
                    ) {
                      Column(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "常用网站",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "长按可移除",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkTertiary,
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // 添加按钮固定在最后一行的行尾；不满的行用等宽空位补齐，
                        // 保证各列与首行对齐（尾行靠左，不居中）。
                        val rows = remember(shortcuts.size) {
                            (shortcuts.map<Shortcut, Shortcut?> { it } + null).chunked(4)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 168.dp),
                        ) {
                            rows.forEachIndexed { rowIndex, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    row.forEach { site ->
                                        if (site == null) {
                                            AddShortcutItem(onClick = onAddShortcut)
                                        } else {
                                            ShortcutItem(
                                                site = site,
                                                onClick = { onOpenUrl(site.url) },
                                                onLongClick = { onRemoveShortcut(site.url) },
                                            )
                                        }
                                    }
                                    repeat(4 - row.size) { Spacer(Modifier.width(62.dp)) }
                                }
                                if (rowIndex != rows.lastIndex) Spacer(Modifier.height(14.dp))
                            }
                        }
                      }
                    }
                }

                Spacer(Modifier.weight(1f))
                PrivacyFooter()
                Spacer(Modifier.height(108.dp))
            }

            AnxinActionBar(
                actions = actions,
                tabCount = tabCount,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit,
    searchEngine: String,
    onPickEngine: () -> Unit,
) {
    val palette = AnxinPalette
    var query by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .glassPanel(radius = 26.dp, alpha = 0.82f, borderAlpha = 0.95f, shadowElevation = 16.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = "切换搜索引擎：$searchEngine",
            tint = palette.brand,
            modifier = Modifier.clickable(onClick = onPickEngine).padding(6.dp).size(21.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(
                color = palette.ink,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            ),
            cursorBrush = SolidColor(palette.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            modifier = Modifier.imeTap().weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "搜索或输入网址",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkTertiary,
                    )
                }
                inner()
            },
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.TextButton(
            onClick = { onSearch(query) },
            enabled = query.isNotBlank(),
        ) { Text("搜索") }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ShortcutItem(
    site: Shortcut,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = AnxinPalette
    val color = Color(site.color)
    Column(
        modifier = Modifier
            .width(62.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                site.badge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            site.label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.ink.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 最近使用条目配色（与快捷入口同一视觉体系，但走品牌紫蓝系以示区分）。 */
private val recentPalette = listOf(
    0xFF7B5CFFL, 0xFF3D6BFFL, 0xFF23C08AL, 0xFFE1251BL,
    0xFFFB7299L, 0xFF57C7FFL, 0xFFFF5A1FL, 0xFF5B6BB5L,
)

@Composable
private fun RecentSiteItem(
    title: String,
    url: String,
    color: Long,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    val badge = remember(title, url) {
        title.trim().take(1).uppercase().ifBlank { "?" }
    }
    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(color)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                badge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            title.ifBlank { url.substringAfter("://").substringBefore("/") },
            style = MaterialTheme.typography.labelSmall,
            color = palette.ink.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrivacyFooter() {
    val palette = AnxinPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = palette.inkTertiary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "本地数据 · 不上传浏览记录",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkTertiary,
        )
    }
}

@Composable
private fun AddShortcutItem(onClick: () -> Unit) {
    val palette = AnxinPalette
    Column(
        modifier = Modifier
            .width(62.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isAnxinDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.62f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "添加快捷入口",
                tint = if (isAnxinDark) Color.White else palette.brand,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "添加",
            style = MaterialTheme.typography.labelSmall,
            color = palette.inkSecondary,
        )
    }
}

