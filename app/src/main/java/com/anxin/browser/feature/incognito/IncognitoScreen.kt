package com.anxin.browser.feature.incognito

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.SystemBarsAppearance
import com.anxin.browser.design.glassPanel
import com.anxin.browser.design.imeTap
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions

/**
 * 无痕新开页：搜索/输入网址直接以无痕标签打开。
 * 旧的「开始无痕浏览」按钮层替换为搜索栏。
 */
@Composable
fun IncognitoScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    actions: NavActions,
    tabCount: Int,
) {
    SystemBarsAppearance(lightIcons = true)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF241A3E), Color(0xFF17121F))
                )
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    Icons.Rounded.ArrowBack,
                    "返回",
                    size = 36.dp,
                    iconTint = Color.White,
                    onClick = onBack,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .glassPanel(radius = 18.dp, alpha = 0.24f, borderAlpha = 0.4f, shadowElevation = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "隐私模式",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Spacer(Modifier.weight(0.85f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF8A6BFF).copy(alpha = 0.40f),
                                        Color(0xFF5B3BD6).copy(alpha = 0.10f),
                                        Color.Transparent,
                                    )
                                )
                            ),
                    )
                    Icon(
                        Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "无痕浏览",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "不会保存浏览记录\n关闭后将自动清除",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(26.dp))

                // 无痕搜索栏：输入关键词或网址，点「搜索」或回车即以无痕标签打开。
                var input by rememberSaveable { mutableStateOf("") }
                // 进入页面自动聚焦并拉起键盘：无痕页的核心动作就是"搜点什么"，
                // 让用户少点一下。部分 ROM 上首次组合的输入框不主动弹键盘，
                // 所以显式 requestFocus + show 兜底。
                val searchFocus = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(350)
                    runCatching {
                        searchFocus.requestFocus()
                        keyboard?.show()
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(27.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .glassPanel(radius = 27.dp, alpha = 0.22f, borderAlpha = 0.35f, shadowElevation = 10.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (input.isBlank()) {
                            Text(
                                "搜索或输入网址（无痕）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.45f),
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                            ),
                            cursorBrush = SolidColor(Color(0xFF8A6BFF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (input.isNotBlank()) {
                                    onSearch(input.trim())
                                    input = ""
                                }
                            }),
                            modifier = Modifier.imeTap().fillMaxWidth().focusRequester(searchFocus),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    val canSearch = input.isNotBlank()
                    Text(
                        "搜索",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (canSearch) Color.White else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (canSearch) Color(0xFF6D5BD0) else Color.White.copy(alpha = 0.12f)
                            )
                            .clickable(enabled = canSearch) {
                                onSearch(input.trim())
                                input = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PrivacyHint(Icons.Rounded.History, "不记录历史")
                    PrivacyHint(Icons.Rounded.Storage, "不保留 Cookie")
                    PrivacyHint(Icons.Rounded.Lock, "关闭即清理")
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(96.dp))
        }

        AnxinActionBar(
            actions = actions,
            tabCount = tabCount,
            dark = true,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PrivacyHint(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.48f))
    }
}
