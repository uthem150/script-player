package dev.uthem.scriptplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.uthem.scriptplayer.R

/**
 * Pretendard 가변 폰트.
 *
 * 굵기별 파일을 따로 넣지 않고 **한 파일에 굵기를 지정해서** 쓴다(가변 폰트, API 26+).
 * 정적 파일 넷을 넣는 것보다 용량이 적고, 굵기를 하나 더 쓰고 싶을 때 파일을 추가하지 않아도 된다.
 *
 * 축소판(Std, 2350자)이 아니라 전체판을 쓴다. 대본에 드문 음절이 하나라도 나오면 두부(□)가
 * 뜨는데, 본문이 읽히는 것이 이 앱의 핵심이라 6.4MB 를 감당하는 쪽이 맞다.
 */
internal val Pretendard = FontFamily(
    Font(
        resId = R.font.pretendard_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.pretendard_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.pretendard_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.pretendard_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/**
 * 타이포 눈금.
 *
 * Compass 는 데스크톱 업무 도구라 본문이 13~14px 로 촘촘하다. 이쪽은 폰에서 읽으며 듣는
 * 화면이므로 한 단계 키웠다.
 *
 * `bodyLarge` 가 대본 본문 자리다 — 걸으면서도 읽히도록 18sp 에 행간을 1.7 로 벌린다.
 * 현재 문장을 강조할 때 **크기를 키우지 않는다.** 크기가 바뀌면 줄 높이가 달라져 레이아웃이
 * 튀고, 그러면 자동 스크롤이 어긋난다. 강조는 배경과 굵기로만 한다.
 */
internal val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.heading,
        lineHeight = 31.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.title,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.lg,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    titleSmall = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    // 대본 본문
    bodyLarge = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.script,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.body,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.sm,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = Pretendard,
        fontSize = FontSize.xs,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
)
