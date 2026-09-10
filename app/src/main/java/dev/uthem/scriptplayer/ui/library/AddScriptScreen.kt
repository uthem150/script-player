package dev.uthem.scriptplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.ui.component.AppButton
import dev.uthem.scriptplayer.ui.component.AppOutlinedButton
import dev.uthem.scriptplayer.ui.component.AppTextField
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 새 대본 붙여넣기.
 *
 * 시트가 아니라 화면 한 장을 쓴다. 대본은 수십 줄이라 시트에 담으면 무엇을 붙여넣었는지
 * 보이지 않고, 키보드가 올라오면 남는 자리가 몇 줄뿐이다.
 */
@Composable
fun AddScriptScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .padding(Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x3),
    ) {
        Text(
            "새 대본",
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            "마크다운 기호와 코드 블록은 알아서 걸러냅니다. 화자가 나뉘어 있으면 " +
                "다른 목소리로 읽습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )

        AppTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = "대본을 붙여넣으세요",
            minLines = 8,
        )

        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.negative,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.x2, Alignment.End),
        ) {
            AppOutlinedButton(text = "취소", onClick = onCancel)
            AppButton(text = "담기", onClick = onSave, enabled = text.isNotBlank())
        }
    }
}

@Preview
@Composable
private fun AddScriptPreview() {
    AppTheme(darkTheme = false) {
        AddScriptScreen(text = "", onTextChange = {}, onSave = {}, onCancel = {})
    }
}

@Preview
@Composable
private fun AddScriptErrorPreview() {
    AppTheme(darkTheme = false) {
        AddScriptScreen(
            text = "```\nconsole.log('a')\n```",
            onTextChange = {},
            onSave = {},
            onCancel = {},
            error = "읽을 문장이 없습니다. 코드 블록과 표는 소리로 읽지 않습니다.",
        )
    }
}
