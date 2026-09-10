package dev.uthem.scriptplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

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

internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)
