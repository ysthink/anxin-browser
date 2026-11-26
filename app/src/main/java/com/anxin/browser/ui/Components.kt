package com.anxin.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.anxinSurfaceMuted
import com.anxin.browser.design.glassPanel

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Rounded.Home),
    TABS("标签", Icons.Rounded.Tab),
    EXTENSIONS("扩展", Icons.Rounded.Extension),
    DOWNLOADS("下载", Icons.Rounded.Download),
    PROFILE("我的", Icons.Rounded.Person),
}

@Composable
fun AnxinBottomBar(
    active: BottomTab,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AnxinPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(72.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 10.dp)
                .glassPanel(radius = 34.dp, alpha = 0.62f, borderAlpha = 0.85f, shadowElevation = 20.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = tab == active
                val center = tab == BottomTab.EXTENSIONS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (center) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    clip = false,
                                    ambientColor = palette.brand.copy(alpha = 0.5f),
                                    spotColor = palette.brand.copy(alpha = 0.6f),
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(palette.brandSoft, palette.brand, palette.violet),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = palette.onAccent,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    } else {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) palette.brand else palette.inkTertiary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    if (!center) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) palette.brandDeep else palette.inkSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    count: Int? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = AnxinPalette.ink,
        )
        if (count != null) {
            Spacer(Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(anxinSurfaceMuted())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = AnxinPalette.inkSecondary,
                )
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.size(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AnxinPalette.inkTertiary)
        }
    }
}
