package dev.uthem.scriptplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Radius
import kotlinx.coroutines.launch

/**
 * 오른쪽에 붙는 스크롤 손잡이.
 *
 * 두 가지를 한 자리에서 한다.
 * - **끌거나 눌러서 빠르게 이동.** 대본이 백 문장을 넘으면 손가락으로 밀어서는 원하는
 *   데까지 한참 걸린다
 * - **지금 듣는 자리 표시.** 회색 손잡이는 보고 있는 자리를, 파란 눈금은 **재생 중인
 *   문장**을 가리킨다. 둘이 떨어져 있으면 "지금 다른 데를 보고 있다" 가 한눈에 보인다
 *
 * 픽셀 높이가 아니라 **문장 번호**를 기준으로 잰다. 문장마다 높이가 달라 픽셀로는
 * 전체 길이를 알 수 없고, 문장 번호는 재생 위치와 같은 눈금이라 두 표시를 같은 자로 그린다.
 */
@Composable
fun ScriptScrollbar(
    listState: LazyListState,
    sentenceCount: Int,
    playingIndex: Int,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 6.dp,
    thumbHeight: Dp = 48.dp,
) {
    // 한 화면에 다 들어오는 대본에는 손잡이가 필요 없다
    if (sentenceCount <= 1) return

    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            // 손가락이 닿을 만큼 넓게 잡는다. 눈에 보이는 홈은 그보다 얇다
            .width(32.dp)
            .padding(vertical = 6.dp),
    ) {
        val trackHeight = maxHeight
        val travel = trackHeight - thumbHeight
        val colors = AppTheme.colors

        fun jumpTo(offsetY: Float, heightPx: Float) {
            val target = indexForOffset(offsetY, heightPx, sentenceCount)
            scope.launch { listState.scrollToItem(target) }
        }

        val gestures = Modifier
            .pointerInput(sentenceCount, trackHeight) {
                detectVerticalDragGestures { change, _ ->
                    jumpTo(change.position.y, size.height.toFloat())
                }
            }
            .pointerInput(sentenceCount, trackHeight) {
                // 눌러서 곧바로 그 자리로 — 끌지 않아도 되게
                detectTapGestures { position -> jumpTo(position.y, size.height.toFloat()) }
            }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(32.dp)
                .then(gestures)
                .semantics { contentDescription = "대본 위치 손잡이" },
        ) {
            // 홈
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(trackWidth)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(colors.surfaceSunken),
            )

            // 재생 중인 문장 — 손잡이와 떨어져 있으면 다른 데를 보고 있다는 뜻
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = travel * scrollRatio(playingIndex, sentenceCount))
                    .width(trackWidth)
                    .height(PlayingMarkHeight)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(colors.primary),
            )

            // 손잡이 — 지금 보고 있는 자리
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = travel * scrollRatio(listState.firstVisibleItemIndex, sentenceCount))
                    .width(trackWidth)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(colors.borderStrong),
            )
        }
    }
}

private val PlayingMarkHeight = 6.dp

/**
 * 문장 번호를 0~1 비율로.
 *
 * 마지막 문장에서 1이 되게 `count - 1` 로 나눈다. `count` 로 나누면 끝까지 내려도
 * 손잡이가 바닥에 닿지 않아 "아직 더 있나" 싶어진다.
 */
internal fun scrollRatio(index: Int, count: Int): Float {
    if (count <= 1) return 0f
    return (index.toFloat() / (count - 1)).coerceIn(0f, 1f)
}

/** 누르거나 끈 자리에서 갈 문장 번호. */
internal fun indexForOffset(offsetPx: Float, trackHeightPx: Float, sentenceCount: Int): Int {
    if (trackHeightPx <= 0f || sentenceCount <= 1) return 0
    val ratio = (offsetPx / trackHeightPx).coerceIn(0f, 1f)
    return (ratio * (sentenceCount - 1)).toInt().coerceIn(0, sentenceCount - 1)
}
