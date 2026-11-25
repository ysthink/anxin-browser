package com.anxin.browser.feature.incognito

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anxin.browser.design.AuroraBackground
import com.anxin.browser.design.GlassIconButton
import com.anxin.browser.design.GlassPrimaryButton
import com.anxin.browser.design.SystemBarsAppearance
import com.anxin.browser.design.glassPanel
import com.anxin.browser.ui.AnxinActionBar
import com.anxin.browser.ui.NavActions

@Composable
fun IncognitoScreen(
    onBack: () -> Unit,
    onStart: () -> Unit,
    actions: NavActions,
    tabCount: Int,
) {
    SystemBarsAppearance(lightIcons = true)
    AuroraBackground(dark = true) {
      Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                GlassIconButton(
                    Icons.Rounded.Lock,
                    "隐私锁定",
                    size = 36.dp,
                    iconTint = Color.White,
                    onClick = {},
                )
            }

            Spacer(Modifier.weight(0.9f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF8A6BFF).copy(alpha = 0.45f),
                                        Color(0xFF5B3BD6).copy(alpha = 0.12f),
                                        Color.Transparent,
                                    )
                                )
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .glassPanel(radius = 48.dp, alpha = 0.10f, borderAlpha = 0.30f, shadowElevation = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
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
                Spacer(Modifier.height(30.dp))
                GlassPrimaryButton(
                    text = "开始无痕浏览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    onClick = onStart,
                )
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
}

@Composable
private fun PrivacyHint(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.48f))
    }
}
