package com.anxin.browser.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 安心浏览器设计令牌。
 *
 * 视觉基调：液态玻璃（Liquid Glass）—— 半透明层 + 边光 + 品牌蓝紫渐变。
 */
@Immutable
data class AnxinColors(
    val brand: Color = Color(0xFF3D6BFF),
    val brandDeep: Color = Color(0xFF2A46C9),
    val brandSoft: Color = Color(0xFF8FB0FF),
    val violet: Color = Color(0xFF7B5CFF),
    val cyan: Color = Color(0xFF57C7FF),
    val ink: Color = Color(0xFF0D1730),
    val inkSecondary: Color = Color(0xFF6B7B98),
    val inkTertiary: Color = Color(0xFF9AA8BE),
    val positive: Color = Color(0xFF1FBF8F),
    val warning: Color = Color(0xFFFF9F43),
    val danger: Color = Color(0xFFFF5A5F),
    val glassTint: Color = Color(0xFFFFFFFF),
    val hairline: Color = Color(0x33FFFFFF),
    val pageTop: Color = Color(0xFFDCE8FF),
    val pageMid: Color = Color(0xFFEDF2FF),
    val pageBottom: Color = Color(0xFFF7F2FF),
)

@Immutable
data class AnxinDarkColors(
    val brand: Color = Color(0xFF7C9BFF),
    val violet: Color = Color(0xFF9D7BFF),
    val ink: Color = Color(0xFFF2F5FF),
    val inkSecondary: Color = Color(0xFFB9C4E0),
    val pageTop: Color = Color(0xFF120E2A),
    val pageMid: Color = Color(0xFF241A4A),
    val pageBottom: Color = Color(0xFF0B0A1F),
    val glassTint: Color = Color(0xFFFFFFFF),
)

val LocalAnxinColors = staticCompositionLocalOf { AnxinColors() }
val LocalAnxinDarkColors = staticCompositionLocalOf { AnxinDarkColors() }

/** 统一圆角 / 间距令牌。 */
object AnxinShapes {
    val xs = 10.dp
    val sm = 14.dp
    val md = 18.dp
    val lg = 22.dp
    val xl = 28.dp
    val xxl = 34.dp
}

private val AnxinTypography = Typography(
    displayLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

@Composable
fun AnxinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = AnxinColors()
    val scheme = lightColorScheme(
        primary = colors.brand,
        onPrimary = Color.White,
        secondary = colors.violet,
        background = colors.pageMid,
        surface = Color.White,
        onSurface = colors.ink,
        onSurfaceVariant = colors.inkSecondary,
    )
    CompositionLocalProvider(LocalAnxinColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AnxinTypography,
            content = content,
        )
    }
}

/** 便捷访问器。 */
val AnxinPalette: AnxinColors
    @Composable get() = LocalAnxinColors.current
