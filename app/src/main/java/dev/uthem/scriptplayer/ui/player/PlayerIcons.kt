package dev.uthem.scriptplayer.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 재생 컨트롤 도형을 직접 그린다.
 *
 * 처음에 유니코드 글자(`⏸`·`⏴`)를 썼는데 스크린샷에서 틀린 것이 드러났다 — Pretendard 에
 * 없는 글자는 두부(□)로 나오고, `⏸` 는 컬러 이모지로 렌더돼 파란 단추 위에 주황색이
 * 얹혔다. 폰트에 있는지 없는지에 모양이 달린 것은 못 믿는다.
 *
 * 아이콘 묶음(material-icons)을 받는 대신 그린다. 필요한 모양이 네 개뿐이고, 그 라이브러리는
 * 수 MB 인데 우리가 쓰는 것은 그중 넷이다.
 */

@Composable
fun PlayIcon(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width * 0.24f, 0f)
            lineTo(this@Canvas.size.width * 0.24f, this@Canvas.size.height)
            lineTo(this@Canvas.size.width * 0.92f, this@Canvas.size.height / 2f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun PauseIcon(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val barWidth = this.size.width * 0.26f
        val gap = this.size.width * 0.18f
        val left = (this.size.width - (barWidth * 2 + gap)) / 2f
        val radius = CornerRadius(barWidth * 0.25f)
        drawRoundRect(
            color = color,
            topLeft = Offset(left, 0f),
            size = Size(barWidth, this.size.height),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(left + barWidth + gap, 0f),
            size = Size(barWidth, this.size.height),
            cornerRadius = radius,
        )
    }
}

/** 문장 이동 — 삼각형에 막대를 붙인 모양. [forward] 가 거짓이면 좌우를 뒤집는다. */
@Composable
fun SkipIcon(
    color: Color,
    forward: Boolean,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        val barWidth = width * 0.16f
        val triangleWidth = width - barWidth - width * 0.08f

        fun x(value: Float) = if (forward) value else width - value

        val triangle = Path().apply {
            moveTo(x(0f), 0f)
            lineTo(x(0f), height)
            lineTo(x(triangleWidth), height / 2f)
            close()
        }
        drawPath(triangle, color)
        drawRoundRect(
            color = color,
            topLeft = Offset(if (forward) width - barWidth else 0f, 0f),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth * 0.3f),
        )
    }
}
