package com.anxin.browser.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃面板：柔和投影 + 垂直半透明填充 + 顶部高光 + 渐变描边。
 */
@Composable
fun Modifier.glassPanel(
    radius: Dp = AnxinShapes.lg,
    alpha: Float = 0.52f,
    borderAlpha: Float = 0.72f,
    shadowElevation: Dp = 16.dp,
    tint: Color? = null,
    shadowTint: Color = Color(0xFF3D6BFF),
): Modifier {
    val shape: Shape = RoundedCornerShape(radius)
    // 玻璃底色跟随主题：亮色为白，深色为深蓝紫。否则深色模式下
    // 白色半透明面板会把 palette.ink（已变白）的文字吃掉。
    val palette = AnxinPalette
    val panelTint = tint ?: palette.glassTint
    val lineColor = if (isAnxinDark && tint == null) {
        palette.hairline.copy(alpha = borderAlpha)
    } else {
        Color.White.copy(alpha = borderAlpha)
    }
    return this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = shadowTint.copy(alpha = 0.20f),
            spotColor = shadowTint.copy(alpha = 0.32f),
        )
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    panelTint.copy(alpha = (alpha + 0.16f).coerceAtMost(1f)),
                    panelTint.copy(alpha = alpha * 0.66f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        )
        .background(
            Brush.verticalGradient(
                0f to palette.glassHighlight,
                0.45f to Color.Transparent,
                1f to palette.hairline.copy(alpha = 0.28f),
            )
        )
        .border(
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        lineColor,
                        lineColor.copy(alpha = 0.22f),
                        lineColor.copy(alpha = 0.75f),
                    ),
                ),
            ),
            shape = shape,
        )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = AnxinShapes.lg,
    alpha: Float = 0.52f,
    borderAlpha: Float = 0.72f,
    shadowElevation: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .glassPanel(radius = radius, alpha = alpha, borderAlpha = borderAlpha, shadowElevation = shadowElevation)
            .padding(contentPadding),
    ) {
        content()
    }
}

/** 圆形玻璃图标按钮。 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconTint: Color = AnxinPalette.ink,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .glassPanel(radius = size / 2, alpha = 0.5f, borderAlpha = 0.8f, shadowElevation = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** 主按钮：品牌渐变 + 外发光。 */
@Composable
fun GlassPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = palette.brand.copy(alpha = 0.45f),
                spotColor = palette.brand.copy(alpha = 0.55f),
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(palette.brandSoft, palette.brand, palette.violet)
                    } else {
                        listOf(palette.surfaceMuted, palette.surface)
                    },
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.1f))),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor = if (enabled) palette.onAccent else palette.inkTertiary
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}

/** 分段选择器单个胶囊。 */
@Composable
fun GlassSegment(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = AnxinPalette
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    // 深色下白底会把亮色系品牌文字吃掉，改用品牌色填充 + 白字。
                    if (isAnxinDark) {
                        Brush.horizontalGradient(listOf(palette.brand, palette.violet))
                    } else {
                        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.78f)))
                    }
                } else {
                    Brush.horizontalGradient(listOf(anxinField(), anxinSurfaceMuted()))
                }
            )
            .border(
                1.dp,
                if (selected) palette.hairline else anxinDivider(),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = when {
                selected && isAnxinDark -> palette.onAccent
                selected -> palette.brandDeep
                else -> palette.inkSecondary
            },
        )
    }
}

/** 通用小标签。 */
@Composable
fun GlassTag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = anxinField(),
    textColor: Color = AnxinPalette.inkSecondary,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

/** 区块标题 + 右侧动作。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AnxinPalette.ink)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(action, style = MaterialTheme.typography.bodySmall, color = AnxinPalette.inkSecondary)
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AnxinPalette.inkTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** 细进度条。 */
@Composable
fun ThinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    track: Color = anxinDivider(),
    brush: Brush? = null,
) {
    val palette = AnxinPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(brush ?: Brush.horizontalGradient(listOf(palette.brand, palette.violet))),
        )
    }
}

/** 彩色圆角图标底座（列表行首图标）。 */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    container: Color = tint.copy(alpha = 0.14f),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** 单行文本省略。 */
@Composable
fun OneLineText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = AnxinPalette.ink,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun CircleDot(color: Color, size: Dp = 6.dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun MinTouchTarget(content: @Composable () -> Unit) {
    Box(Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
fun ScreenColumnPadding(): PaddingValues = PaddingValues(horizontal = 18.dp)

/** 可横向滚动的分段选择器组，避免 4 个以上选项在窄屏被挤压或截断。 */
@Composable
fun GlassSegments(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            GlassSegment(label, selectedIndex == index) { onSelect(index) }
        }
    }
}

/** 统一的空状态：图标 + 主标题 + 说明 + 可选主操作。 */
@Composable
fun AnxinEmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().glassPanel(radius = AnxinShapes.lg, alpha = 0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBubble(icon, AnxinPalette.brand, size = 56.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = AnxinPalette.ink)
        Spacer(Modifier.height(6.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = if (isAnxinDark) AnxinPalette.ink.copy(alpha = 0.86f) else AnxinPalette.inkSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            GlassPrimaryButton(text = actionLabel, onClick = onAction)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 弹窗统一容器色，避免夜间模式出现刺眼白卡。 */
val AnxinDialogContainer: Color
    @Composable get() = if (isAnxinDark) Color(0xFF241E44) else Color(0xFFFDFEFF)
