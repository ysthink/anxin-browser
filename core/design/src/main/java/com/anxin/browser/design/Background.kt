package com.anxin.browser.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot
import kotlin.random.Random

/**
 * 极光渐变背景：模拟液态玻璃底层的柔和色斑，避免纯色板结。
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = AnxinPalette
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            if (dark) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color(0xFF171034),
                        0.42f to Color(0xFF2B1C5C),
                        0.72f to Color(0xFF1A1140),
                        1f to Color(0xFF0A0818),
                    )
                )
                blob(Offset(size.width * 0.18f, size.height * 0.10f), size.width * 0.62f, Color(0xFF6C4BFF), 0.42f)
                blob(Offset(size.width * 0.92f, size.height * 0.30f), size.width * 0.58f, Color(0xFFB25BFF), 0.30f)
                blob(Offset(size.width * 0.55f, size.height * 0.86f), size.width * 0.70f, Color(0xFF3F6BFF), 0.28f)
                stars()
            } else {
                drawRect(
                    Brush.verticalGradient(
                        0f to palette.pageTop,
                        0.45f to palette.pageMid,
                        1f to palette.pageBottom,
                    )
                )
                blob(Offset(size.width * 0.10f, size.height * 0.06f), size.width * 0.70f, Color(0xFFA9C4FF), 0.55f)
                blob(Offset(size.width * 0.96f, size.height * 0.22f), size.width * 0.62f, Color(0xFFC9B6FF), 0.45f)
                blob(Offset(size.width * 0.72f, size.height * 0.72f), size.width * 0.66f, Color(0xFF9FE3FF), 0.34f)
                blob(Offset(size.width * 0.22f, size.height * 0.95f), size.width * 0.60f, Color(0xFFD8C7FF), 0.32f)
            }
        }
        content()
    }
}

private fun DrawScope.blob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.stars() {
    val random = Random(7)
    repeat(90) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height * 0.7f
        val r = random.nextFloat() * 1.4f + 0.4f
        val d = hypot(x - size.width / 2f, y - size.height * 0.3f)
        val alpha = (0.35f * (1f - d / size.width)).coerceIn(0.04f, 0.55f)
        drawCircle(Color.White.copy(alpha = alpha), radius = r, center = Offset(x, y))
    }
}
