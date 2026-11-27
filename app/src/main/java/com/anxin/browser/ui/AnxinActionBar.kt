package com.anxin.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.design.AnxinPalette
import com.anxin.browser.design.glassPanel
import com.anxin.browser.design.isAnxinDark

/**
 * 全应用统一的悬浮浏览操作栏。
 *
 * 五个位置：后退 / 前进 / 标签页 / 菜单 / 场景快捷入口。
 *
 * 关键点：它是一个**纯浮层**——不占任何布局高度，也不铺底衬渐变，
 * 网页可以一直画到屏幕最底部，工具栏像胶囊一样浮在上面。
 * 调用方只要把它放进 Box 的 `Alignment.BottomCenter` 即可。
 */
data class NavActions(
    val back: () -> Unit,
    val forward: () -> Unit,
    val tabs: () -> Unit,
    val menu: () -> Unit,
    val newTab: () -> Unit,
    val primaryLabel: String = "新建标签",
    val primaryIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Layers,
)

@Composable
fun AnxinActionBar(
    actions: NavActions,
    modifier: Modifier = Modifier,
    tabCount: Int = 0,
    canGoForward: Boolean = true,
    /** null = 跟随全局主题；true/false = 沉浸页强制指定。 */
    dark: Boolean? = null,
) {
    val useDark = dark ?: isAnxinDark
    val palette = AnxinPalette
    val iconTint = if (useDark) Color(0xFFE8ECFF) else Color(0xFF324477)
    val badgeBg = if (useDark) Color(0xFF5E3BC7) else palette.brand

    // 不再有整条渐变衬底：那层半透明底会把网页最后一行压暗，看着像"占了一条高度"。
    // 现在只剩一颗胶囊，四周留 14dp，网页内容直接透到工具栏后面。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(top = 6.dp, bottom = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .glassPanel(
                    radius = 29.dp,
                    alpha = if (useDark) 0.46f else 0.74f,
                    borderAlpha = if (useDark) 0.40f else 0.92f,
                    shadowElevation = 18.dp,
                    tint = if (useDark) Color(0xFF2A2350) else Color.White,
                    shadowTint = if (useDark) Color(0xFF6B4BD6) else Color(0xFF3D6BFF),
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionSlot(Icons.Rounded.ArrowBack, "后退", iconTint, actions.back)
                ActionSlot(
                    Icons.Rounded.ArrowForward,
                    "前进",
                    if (canGoForward) iconTint else iconTint.copy(alpha = 0.32f),
                    actions.forward,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = actions.tabs),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Tab,
                        contentDescription = "多窗口",
                        tint = iconTint,
                        modifier = Modifier.size(23.dp),
                    )
                    if (tabCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 13.dp, y = (-11).dp)
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(badgeBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (tabCount > 9) "9+" else tabCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                ActionSlot(Icons.Rounded.Menu, "菜单", iconTint, actions.menu)
                ActionSlot(actions.primaryIcon, actions.primaryLabel, iconTint, actions.newTab)
            }
        }
    }
}

@Composable
private fun RowScope.ActionSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
    }
}
