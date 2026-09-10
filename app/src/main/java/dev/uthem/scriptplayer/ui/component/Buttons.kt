package dev.uthem.scriptplayer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.ui.theme.AppTheme

/**
 * 단추 최소 높이.
 *
 * Material3 기본은 40dp 인데, 이 앱의 단추는 걸으면서·흔들리면서 눌린다.
 * 접근성 권고인 48dp 를 바닥으로 못 박는다.
 */
private val MinTouchHeight = 48.dp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primary,
            contentColor = AppTheme.colors.onPrimary,
            // Material 기본 꺼진 색은 글자가 거의 안 보인다. 우리 토큰으로 못 박는다
            disabledContainerColor = AppTheme.colors.surfaceSunken,
            disabledContentColor = AppTheme.colors.textTertiary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, SolidColor(AppTheme.colors.borderStrong)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.textPrimary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
