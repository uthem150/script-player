package dev.uthem.scriptplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Radius
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 고르는 칩. 배속 프리셋(`1.0 · 1.25 · 1.5 · 2.0`)과 화자 라벨 자리다.
 *
 * `selectable` 로 감싼다 — 단순히 `clickable` 을 쓰면 화면 낭독기가 "버튼" 이라고만 읽고
 * 지금 골라져 있는지를 알려주지 않는다. 골라진 상태가 색으로만 표시되면 그 정보가 사라진다.
 *
 * 골라진 것을 배경·글자·경계 **셋으로 함께** 표시한다. 색 하나로만 구분하면 밝은 화면에서
 * 옅은 배경 차이를 놓치기 쉽다.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.full)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primarySubtle else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.border, shape)
            .selectable(selected = selected, onClick = onClick)
            // 걸으면서 누르는 칩이라 좁게 두지 않는다
            .defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
            .padding(horizontal = Space.x3, vertical = Space.x2),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.textSecondary,
        )
    }
}
