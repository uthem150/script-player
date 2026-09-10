package dev.uthem.scriptplayer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * 1계층 · 프리미티브 — 뜻이 없는 원시 눈금.
 *
 * 색·간격·반경·글자 크기의 "재료"이며 테마와 무관하게 불변이다.
 * 화면이 직접 쓰지 않는 것은 **색** 뿐이다 — 색만이 테마에 따라 뒤집히므로
 * 별칭(2계층) → 시맨틱(3계층)을 거쳐야 한다. 간격·반경·글자 크기는 두 테마에서 같은 값이라
 * 화면이 이 눈금을 그대로 쓴다.
 *
 * 값은 Compass 의 3계층 토큰을 그대로 옮긴 것이다.
 */
internal object Primitive {

    // Blue (Toss Blue)
    val Blue50 = Color(0xFFE8F3FF)
    val Blue100 = Color(0xFFC9E2FF)
    val Blue200 = Color(0xFF90C2FF)
    val Blue300 = Color(0xFF64A8FF)
    val Blue400 = Color(0xFF4593FC)
    val Blue500 = Color(0xFF3182F6)
    val Blue600 = Color(0xFF2272EB)
    val Blue700 = Color(0xFF1B64DA)

    // Gray — 파랑이 살짝 섞인 중성 회색
    val Gray0 = Color(0xFFFFFFFF)
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF2F4F6)
    val Gray200 = Color(0xFFE5E8EB)
    val Gray300 = Color(0xFFD1D6DB)
    val Gray400 = Color(0xFFB0B8C1)
    val Gray500 = Color(0xFF8B95A1)
    val Gray600 = Color(0xFF6B7684)
    val Gray700 = Color(0xFF4E5968)
    val Gray800 = Color(0xFF333D4B)
    val Gray900 = Color(0xFF191F28)
    val Gray950 = Color(0xFF0F1319)

    /*
     * Ink — 어두운 화면 전용 중성 회색.
     *
     * 밝은 화면의 회색은 파랑이 섞여 있다(Gray800 은 파랑이 빨강보다 24 높다).
     * 흰 바탕에서는 그 기운이 맑게 읽히지만, 넓은 면을 어둡게 채우면 화면 전체가
     * 푸르스름해진다. 어두운 쪽은 채도를 덜어 낸 값을 따로 둔다.
     */
    val Ink900 = Color(0xFF131417)
    val Ink800 = Color(0xFF1C1D21)
    val Ink700 = Color(0xFF24262A)
    val Ink600 = Color(0xFF2C2E33)
    val Ink500 = Color(0xFF3D4046)
    val Ink400 = Color(0xFFADB0B5)
    val Ink300 = Color(0xFFCFD1D4)

    // 상태 색
    val Red500 = Color(0xFFF04452)
    val Red600 = Color(0xFFE02D3C)
    val Green500 = Color(0xFF00C471)
    val Green600 = Color(0xFF00A862)
    val Amber500 = Color(0xFFFFB020)

    // 어두운 화면의 강조 배경 — Blue50 을 그대로 쓰면 눈이 아프다
    val BlueDim = Color(0xFF1B2740)
}

/** 간격 — 4dp 기반. 테마와 무관하므로 화면이 직접 쓴다. */
internal object Space {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x10 = 40.dp
    val x12 = 48.dp
    val x16 = 64.dp
}

/**
 * 반경.
 *
 * Compass 는 업무 도구라 과하지 않게 잡았지만(카드 8px), 이쪽은 듣는 앱이라 조금 크게 둔다.
 */
internal object Radius {
    val sm = 6.dp
    val md = 10.dp
    val lg = 14.dp
    val xl = 20.dp
    val full = 999.dp
}

/**
 * 글자 크기.
 *
 * Compass 는 데스크톱 업무 도구라 본문이 13~14px 로 촘촘하다. 이쪽은 폰에서 읽으며 듣는
 * 화면이므로 한 단계 키운다. 특히 [script] 는 걸으면서도 읽히도록 크게 잡는다.
 */
internal object FontSize {
    val xs = 12.sp
    val sm = 13.sp
    val body = 15.sp
    val lg = 17.sp
    val script = 18.sp
    val title = 20.sp
    val heading = 24.sp
    val display = 28.sp
}

internal object LineHeight {
    val tight = 1.3f
    val normal = 1.5f
    val relaxed = 1.7f
}
