package dev.uthem.scriptplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 화면에서 색을 읽는 창구.
 *
 * `AppTheme.colors.textPrimary` 처럼 쓴다. Material3 의 `MaterialTheme` 과 같은 모양이다.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = (if (darkTheme) DarkAliases else LightAliases).toAppColors()
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/**
 * Material3 컴포넌트도 같은 색을 쓰게 한다.
 *
 * Button·Slider 같은 것을 쓰면서 이걸 안 채우면, 우리 색과 Material 기본 보라색이
 * 한 화면에 섞인다.
 */
private fun AppColors.toColorScheme(darkTheme: Boolean) = if (darkTheme) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primarySubtle,
        onPrimaryContainer = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = textSecondary,
        outline = borderStrong,
        outlineVariant = border,
        error = negative,
        onError = onPrimary,
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primarySubtle,
        onPrimaryContainer = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = textSecondary,
        outline = borderStrong,
        outlineVariant = border,
        error = negative,
        onError = onPrimary,
    )
}

/**
 * 타이포.
 *
 * `bodyLarge` 는 대본 본문 자리다 — 걸으면서도 읽히도록 18sp 에 행간을 1.7 로 벌려 둔다.
 * 현재 문장을 강조할 때 **크기를 키우지 않는다**. 크기가 바뀌면 줄 높이가 달라져 레이아웃이
 * 튀고, 그러면 자동 스크롤이 어긋난다. 강조는 배경과 굵기로만 한다.
 */
internal val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = FontSize.heading,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontSize = FontSize.title,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = FontSize.lg,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = TextStyle(
        fontSize = FontSize.body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // 대본 본문
    bodyLarge = TextStyle(
        fontSize = FontSize.script,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = FontSize.body,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = FontSize.sm,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = FontSize.body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontSize = FontSize.xs,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)

internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
