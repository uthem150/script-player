package dev.uthem.scriptplayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * 2계층 · 별칭 — 프리미티브를 "의도"로 묶는다.
 *
 * 테마 전환의 유일한 지점이다. 어두운 테마는 이 계층만 다시 정의하면 시맨틱(3계층)과
 * 화면이 자동으로 따라온다. 값을 두 벌로 나눠 적지 않고 한 데이터 구조의 두 인스턴스로 둔다 —
 * 필드를 추가하면 양쪽 다 채우지 않으면 컴파일이 안 되므로, 한쪽만 고치는 실수가 막힌다.
 */
@Immutable
internal data class Aliases(
    val bg: Color,
    val surface: Color,
    val surfaceHover: Color,
    val surfaceSunken: Color,
    val fg: Color,
    val fgMuted: Color,
    val fgSubtle: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentHover: Color,
    val accentFg: Color,
    val accentSubtle: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
)

internal val LightAliases = Aliases(
    bg = Primitive.Gray50,
    surface = Primitive.Gray0,
    surfaceHover = Primitive.Gray50,
    surfaceSunken = Primitive.Gray100,
    fg = Primitive.Gray900,
    fgMuted = Primitive.Gray700,
    fgSubtle = Primitive.Gray600,
    border = Primitive.Gray200,
    borderStrong = Primitive.Gray300,
    accent = Primitive.Blue500,
    accentHover = Primitive.Blue600,
    accentFg = Primitive.Gray0,
    accentSubtle = Primitive.Blue50,
    positive = Primitive.Green500,
    negative = Primitive.Red500,
    warning = Primitive.Amber500,
)

internal val DarkAliases = Aliases(
    bg = Primitive.Ink900,
    surface = Primitive.Ink800,
    surfaceHover = Primitive.Ink600,
    surfaceSunken = Primitive.Ink700,
    fg = Primitive.Gray50,
    fgMuted = Primitive.Ink300,
    fgSubtle = Primitive.Ink400,
    border = Primitive.Ink600,
    borderStrong = Primitive.Ink500,
    // 어두운 바탕에서 Blue500 은 탁해 보인다 — 한 단계 밝은 쪽을 쓴다
    accent = Primitive.Blue400,
    accentHover = Primitive.Blue300,
    accentFg = Primitive.Ink900,
    accentSubtle = Primitive.BlueDim,
    positive = Primitive.Green500,
    negative = Primitive.Red500,
    warning = Primitive.Amber500,
)
