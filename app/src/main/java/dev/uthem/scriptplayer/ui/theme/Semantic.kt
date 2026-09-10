package dev.uthem.scriptplayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * 3계층 · 시맨틱(역할) — 화면이 실제로 쓰는 유일한 계층.
 *
 * 별칭·프리미티브를 역할 이름으로 노출한다. 화면과 컴포넌트는 오직 이 계층만 참조한다.
 * `Primitive.Blue500` 을 화면에서 직접 쓰면 어두운 테마에서 그 자리만 안 바뀐다.
 */
@Immutable
data class AppColors(
    /** 화면 바닥 */
    val background: Color,
    /** 바닥 위에 올라가는 판 — 카드, 시트 */
    val surface: Color,
    val surfaceHover: Color,
    /** 바닥보다 더 눌린 자리 — 입력칸, 코드 블록 */
    val surfaceSunken: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val borderStrong: Color,
    /** 기본 동작 — 재생 버튼, 선택된 배속 */
    val primary: Color,
    val primaryHover: Color,
    val onPrimary: Color,
    /** 현재 문장 하이라이트 배경으로도 쓴다 */
    val primarySubtle: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
)

internal fun Aliases.toAppColors() = AppColors(
    background = bg,
    surface = surface,
    surfaceHover = surfaceHover,
    surfaceSunken = surfaceSunken,
    textPrimary = fg,
    textSecondary = fgMuted,
    textTertiary = fgSubtle,
    border = border,
    borderStrong = borderStrong,
    primary = accent,
    primaryHover = accentHover,
    onPrimary = accentFg,
    primarySubtle = accentSubtle,
    positive = positive,
    negative = negative,
    warning = warning,
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors 는 AppTheme 안에서만 읽을 수 있습니다. 화면을 AppTheme 으로 감싸세요.")
}
