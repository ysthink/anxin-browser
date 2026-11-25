package com.anxin.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anxin.browser.design.glassPanel

/**
 * 全应用统一的悬浮浏览操作栏。
 *
 * 五个位置：后退 / 前进 / 标签页 / 菜单 / 场景快捷入口。
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
    dark: Boolean = false,
) {
    val noRipple = remember { MutableInteractionSource() }
    val iconTint = if (dark) Color(0xFFE8ECFF) else Color(0xFF324477)
    val badgeBg = if (dark) Color(0xFF9D7BFF) else Color(0xFF3D6BFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(64.dp)
            .glassPanel(
                radius = 30.dp,
                alpha = if (dark) 0.42f else 0.72f,
                borderAlpha = if (dark) 0.38f else 0.92f,
                shadowElevation = 20.dp,
                tint = if (dark) Color(0xFF2A2350) else Color.White,
                shadowTint = if (dark) Color(0xFF6B4BD6) else Color(0xFF3D6BFF),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionSlot(Icons.Rounded.ArrowBack, "后退", iconTint, noRipple, actions.back)
            ActionSlot(
                Icons.Rounded.ArrowForward,
                "前进",
                if (canGoForward) iconTint else iconTint.copy(alpha = 0.32f),
                noRipple,
                actions.forward,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = noRipple, indication = null, onClick = actions.tabs),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Tab,
                    contentDescription = "标签页",
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

            ActionSlot(Icons.Rounded.Menu, "菜单", iconTint, noRipple, actions.menu)
            ActionSlot(actions.primaryIcon, actions.primaryLabel, iconTint, noRipple, actions.newTab)
        }
    }
}

@Composable
private fun RowScope.ActionSlot(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    source: MutableInteractionSource,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
    }
}
