package dev.uthem.scriptplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 컴포넌트를 눈으로 확인하는 자리.
 *
 * 실제로 쓰일 모양 그대로 담는다 — 보관함 카드, 배속 칩 줄, 대본 붙여넣기 칸.
 * 빈 컴포넌트를 늘어놓으면 정작 화면에서 어떻게 보이는지는 알 수 없다.
 */
@Composable
fun ComponentCatalog(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x4),
    ) {
        Text("컴포넌트", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)

        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            AppButton(text = "재생", onClick = {})
            AppOutlinedButton(text = "정지", onClick = {})
            AppButton(text = "꺼짐", onClick = {}, enabled = false)
        }

        // 배속 칩 줄 — 실제 재생기 하단에 놓일 모양
        Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
            listOf("0.75", "1.0", "1.25", "1.5", "2.0").forEach { speed ->
                AppChip(label = speed, selected = speed == "1.25", onClick = {})
            }
        }

        // 보관함 카드 — 제목·분량·진도
        AppCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
            Text(
                "자바스크립트 이벤트 루프, 한 번에 이해하기",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                "41문장 · 3분 · 62% 들었음",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }

        AppTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "대본을 붙여넣으세요",
            minLines = 3,
        )
    }
}

@Preview
@Composable
private fun ComponentLightPreview() = AppTheme(darkTheme = false) { ComponentCatalog() }

@Preview
@Composable
private fun ComponentDarkPreview() = AppTheme(darkTheme = true) { ComponentCatalog() }
