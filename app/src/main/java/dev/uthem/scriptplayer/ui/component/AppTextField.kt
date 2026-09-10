package dev.uthem.scriptplayer.ui.component

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.uthem.scriptplayer.ui.theme.AppTheme

/**
 * 글자 입력칸. 대본을 붙여넣는 자리와 제목을 고치는 자리에 쓴다.
 *
 * 대본은 길다. 그래서 여러 줄을 기본으로 두고 [minLines] 로 처음 높이를 정한다 —
 * 한 줄로 시작하면 수십 줄을 붙여넣었을 때 무엇이 들어갔는지 확인할 수 없다.
 *
 * 바탕을 `surfaceSunken` 으로 눌러 둔다. 판(`surface`)과 같은 색이면 어디까지가 입력칸인지
 * 경계선만으로 알아야 하고, 어두운 테마에서 그 선이 거의 안 보인다.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    minLines: Int = 1,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = MaterialTheme.shapes.medium,
        placeholder = placeholder?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            disabledContainerColor = colors.surfaceSunken,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.border,
            cursorColor = colors.primary,
            focusedPlaceholderColor = colors.textTertiary,
            unfocusedPlaceholderColor = colors.textTertiary,
            disabledBorderColor = colors.border,
            errorContainerColor = colors.surfaceSunken,
            errorBorderColor = colors.negative,
            // 기본값은 Material 보라색이라 우리 화면에서 튄다
            selectionColors = TextSelectionColors(
                handleColor = colors.primary,
                backgroundColor = colors.primary.copy(alpha = 0.3f),
            ),
        ),
    )
}
