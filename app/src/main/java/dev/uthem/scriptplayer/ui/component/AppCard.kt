package dev.uthem.scriptplayer.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 판 한 장.
 *
 * Material3 의 `Card` 는 그림자로 띄우는데, 여기서는 **경계선으로만** 구분한다.
 * 목록에 카드가 여러 장 쌓이면 그림자가 겹쳐 화면이 지저분해지고, 어두운 테마에서는
 * 그림자가 거의 보이지 않아 구분이 사라진다. 선은 두 테마에서 똑같이 일한다.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // 카드는 대개 제목 위에 부가정보를 쌓는다. 간격을 주지 않았더니 두 줄이 붙어 답답했다.
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Space.x1),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        verticalArrangement = verticalArrangement,
        modifier = modifier
            // 클립을 먼저 걸어야 눌렀을 때 번지는 물결이 모서리를 넘어가지 않는다
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(1.dp, AppTheme.colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Space.x4),
        content = content,
    )
}
