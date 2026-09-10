package dev.uthem.scriptplayer.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * 토큰을 눈으로 확인하는 자리.
 *
 * 색은 값을 읽어서는 맞는지 알 수 없고 나란히 놓고 봐야 안다. 두 테마를 같은 구성으로
 * 찍어 두면 한쪽만 어긋난 것이 바로 보인다. 스크린샷 테스트가 이 화면들을 찍는다.
 *
 * **한 화면에 담기는 만큼만 넣는다.** 처음에 색과 글자를 한 장에 몰았더니 아래쪽 절반이
 * 잘려 나가, 정작 확인하려던 대본 본문이 사진에 없었다. 그리고 배경을 화면 전체로 깔지
 * 않아 앞 테스트의 글자가 위쪽에 비쳤다 — 두 조각 다 fillMaxSize 로 덮는다.
 */

/** 색 조각들 — 표면·강조·경계·상태. */
@Composable
fun ColorCatalog(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    CatalogSurface(modifier) {
        Text("색 토큰", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)

        Group("표면") {
            Swatch("background", colors.background)
            Swatch("surface", colors.surface)
            Swatch("surfaceHover", colors.surfaceHover)
            Swatch("surfaceSunken", colors.surfaceSunken)
        }
        Group("강조") {
            Swatch("primary", colors.primary)
            Swatch("primaryHover", colors.primaryHover)
            Swatch("primarySubtle", colors.primarySubtle)
        }
        Group("경계·상태") {
            Swatch("border", colors.border)
            Swatch("borderStrong", colors.borderStrong)
            Swatch("positive", colors.positive)
            Swatch("negative", colors.negative)
            Swatch("warning", colors.warning)
        }
    }
}

/** 글자 — 대비와 크기 눈금. 칠한 조각이 아니라 실제 바탕 위에 놓고 읽혀야 판단할 수 있다. */
@Composable
fun TypeCatalog(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    CatalogSurface(modifier) {
        Text("글자 눈금", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)

        Group("위계") {
            Text("headlineSmall 24sp", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Text("titleLarge 20sp", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text("titleMedium 17sp", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text("textPrimary · 본문 15sp", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text("textSecondary · 부가 13sp", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text("textTertiary · 부가 13sp", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        }

        Group("대본 본문 18sp · 행간 1.7") {
            Text(
                "이벤트 루프는 콜 스택이 완전히 비워지기 전까지 다른 어떤 일도 끼어들지 " +
                    "못하게 합니다. 그래서 setTimeout 을 0으로 줘도 즉시 실행되지 않습니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            // 현재 문장 강조 — 크기를 키우지 않고 배경과 굵기로만 짚는다.
            // 크기가 바뀌면 줄 높이가 달라져 레이아웃이 튀고, 자동 스크롤이 어긋난다.
            Surface(color = colors.primarySubtle, shape = RoundedCornerShape(Radius.md)) {
                Text(
                    "지금 읽고 있는 문장은 이렇게 배경으로만 짚습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = Space.x3, vertical = Space.x2),
                )
            }
        }
    }
}

@Composable
private fun CatalogSurface(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x4),
        content = { content() },
    )
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textSecondary)
        content()
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        Column(
            modifier = Modifier
                .size(Space.x8)
                .background(color, RoundedCornerShape(Radius.sm))
                // 조각과 바탕이 같은 색일 때(background 처럼) 테두리가 없으면 아무것도 안 보인다
                .border(1.dp, AppTheme.colors.borderStrong, RoundedCornerShape(Radius.sm)),
        ) {}
        Text(name, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textPrimary)
    }
}

@Preview
@Composable
private fun ColorLightPreview() = AppTheme(darkTheme = false) { ColorCatalog() }

@Preview
@Composable
private fun ColorDarkPreview() = AppTheme(darkTheme = true) { ColorCatalog() }

@Preview
@Composable
private fun TypeLightPreview() = AppTheme(darkTheme = false) { TypeCatalog() }

@Preview
@Composable
private fun TypeDarkPreview() = AppTheme(darkTheme = true) { TypeCatalog() }
