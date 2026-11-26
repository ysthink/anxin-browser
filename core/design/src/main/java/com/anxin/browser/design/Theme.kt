package com.anxin.browser.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
    val surface: Color = Color(0xFFF7F9FF),
    val surfaceMuted: Color = Color(0xFFEEF3FC),
    val field: Color = Color(0xFFF2F5FC),
    val divider: Color = Color(0x140D1730),
    val onAccent: Color = Color.White,
    val scrim: Color = Color(0x990A0A1F),
    val glassHighlight: Color = Color(0x66FFFFFF),
)

/**
 * 深色令牌沿用 [AnxinColors] 同一类型，所有以 `AnxinPalette.xxx` 取色的组件
 * 无需改动即可跟随主题。深色下的 brandDeep 用作文字色，因此比亮色模式更亮。
 */
private val AnxinDarkPalette = AnxinColors(
    brand = Color(0xFF8FA8FF),
    brandDeep = Color(0xFFBDD0FF),
    brandSoft = Color(0xFF6479D6),
    violet = Color(0xFFA98BFF),
    cyan = Color(0xFF6BD9FF),
    ink = Color(0xFFF2F5FF),
    inkSecondary = Color(0xFFBBC6E2),
    inkTertiary = Color(0xFF8B98B8),
    positive = Color(0xFF46DBAC),
    warning = Color(0xFFFFB46B),
    danger = Color(0xFFFF7B80),
    glassTint = Color(0xFF2A2350),
    hairline = Color(0x40FFFFFF),
    pageTop = Color(0xFF120E2A),
    pageMid = Color(0xFF241A4A),
    pageBottom = Color(0xFF0B0A1F),
    surface = Color(0xFF241E44),
    surfaceMuted = Color(0xFF2E2654),
    field = Color(0x1FFFFFFF),
    divider = Color(0x26FFFFFF),
    onAccent = Color(0xFF0B0A1F),
    scrim = Color(0xC20A0818),
    glassHighlight = Color(0x2EFFFFFF),
)

val LocalAnxinColors = staticCompositionLocalOf { AnxinColors() }
val LocalAnxinDarkMode = staticCompositionLocalOf { false }

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
    val colors = if (darkTheme) AnxinDarkPalette else AnxinColors()
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.brand,
            onPrimary = Color(0xFF0B0A1F),
            secondary = colors.violet,
            background = colors.pageMid,
            surface = colors.surface,
            surfaceVariant = colors.surfaceMuted,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkSecondary,
            error = colors.danger,
            outline = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = colors.brand,
            onPrimary = Color.White,
            secondary = colors.violet,
            background = colors.pageMid,
            surface = colors.surface,
            surfaceVariant = colors.surfaceMuted,
            onSurface = colors.ink,
            onSurfaceVariant = colors.inkSecondary,
            error = colors.danger,
            outline = colors.divider,
        )
    }
    CompositionLocalProvider(
        LocalAnxinColors provides colors,
        LocalAnxinDarkMode provides darkTheme,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AnxinTypography) {
            // 夜间模式全局生效；播放器 / 无痕等沉浸页可在内层再覆盖。
            SystemBarsAppearance(lightIcons = darkTheme)
            content()
        }
    }
}

/** 便捷访问器。 */
val AnxinPalette: AnxinColors
    @Composable get() = LocalAnxinColors.current

/** 当前是否处于深色主题，供需要走深色分支的组件读取。 */
val isAnxinDark: Boolean
    @Composable get() = LocalAnxinDarkMode.current

/** 语义化表面色。[dark] 可覆盖全局主题，供无痕 / 播放器等强制深色页使用。 */
@Composable
fun anxinSurface(dark: Boolean = isAnxinDark): Color =
    if (dark) Color(0xFF241E44) else Color(0xFFF7F9FF)

@Composable
fun anxinSurfaceMuted(dark: Boolean = isAnxinDark): Color =
    if (dark) Color(0xFF302856) else Color(0xFFEEF3FC)

@Composable
fun anxinField(dark: Boolean = isAnxinDark): Color =
    if (dark) Color(0x29FFFFFF) else Color(0xFFF1F5FC)

@Composable
fun anxinDivider(dark: Boolean = isAnxinDark): Color =
    if (dark) Color(0x29FFFFFF) else Color(0x140D1730)

@Composable
fun anxinOnAccent(): Color = Color.White
