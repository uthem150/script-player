package dev.uthem.scriptplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.ui.component.AppChip
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Radius
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 재생기 화면. 상태를 받기만 한다.
 *
 * 대본을 **정리된 문장으로** 보여준다. 원문을 그대로 띄우면 `**` 와 `##` 가 눈에 들어오고,
 * 들으면서 따라 읽는 화면에서 그것은 방해다. 원문으로 돌아가는 길은 저장된 raw 가 맡는다.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onTapWord: (sentenceIndex: Int, charOffset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding(),
    ) {
        PlayerHeader(title = state.title, onBack = onBack)

        if (state.readySentences < state.sentenceCount) {
            SynthesisBar(ready = state.readySentences, total = state.sentenceCount)
        }

        ScriptBody(
            state = state,
            onTapWord = onTapWord,
            modifier = Modifier.weight(1f),
        )

        PlayerControls(
            state = state,
            onTogglePlay = onTogglePlay,
            onRewind = onRewind,
            onForward = onForward,
            onPreviousSentence = onPreviousSentence,
            onNextSentence = onNextSentence,
            onSeek = onSeek,
            onSpeed = onSpeed,
        )
    }
}

@Composable
private fun PlayerHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x3, vertical = Space.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.x2),
    ) {
        RoundButton(description = "보관함으로", onClick = onBack, size = 44.dp) {
            // 화살표는 Pretendard 에 있는 글자다 — 도형까지 그릴 필요는 없다
            Text("←", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 합성이 아직 진행 중일 때만 뜬다. 실측에서 재생보다 33배 빨라 곧 사라진다. */
@Composable
private fun SynthesisBar(ready: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.x4, vertical = Space.x1),
        verticalArrangement = Arrangement.spacedBy(Space.x1),
    ) {
        Text(
            "소리 준비 중 · $ready / $total 문장",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
        val shape = RoundedCornerShape(Radius.full)
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(shape)
                .background(AppTheme.colors.surfaceSunken),
        ) {
            if (total > 0 && ready > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(ready.toFloat() / total)
                        .height(3.dp)
                        .background(AppTheme.colors.primary, shape),
                )
            }
        }
    }
}

@Composable
private fun ScriptBody(
    state: PlayerUiState,
    onTapWord: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    /*
     * 현재 문장을 따라 스크롤한다.
     *
     * 사용자가 손으로 스크롤하는 동안에는 따라가지 않는다 — 읽던 자리에서 화면이
     * 끌려가면 아무것도 읽을 수 없다.
     */
    LaunchedEffect(state.currentIndex, state.followCurrent) {
        if (state.followCurrent && state.currentIndex in 0 until state.sentences.size) {
            listState.animateScrollToItem(
                index = state.currentIndex,
                // 현재 문장을 화면 위쪽 1/3 쯤에 둔다. 맨 위에 붙이면 다음 문장이 안 보인다
                scrollOffset = -120,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Space.x4, vertical = Space.x2),
        verticalArrangement = Arrangement.spacedBy(Space.x2),
    ) {
        itemsIndexed(state.sentences, key = { index, _ -> index }) { index, sentence ->
            SentenceRow(
                text = sentence.text,
                speakerLabel = sentence.speakerLabel,
                isCurrent = index == state.currentIndex,
                failed = index in state.failedSentences,
                onTapOffset = { offset -> onTapWord(index, offset) },
            )
        }
    }
}

@Composable
private fun SentenceRow(
    text: String,
    speakerLabel: String?,
    isCurrent: Boolean,
    failed: Boolean,
    onTapOffset: (Int) -> Unit,
) {
    val colors = AppTheme.colors
    // 탭 좌표를 글자 번호로 바꾸려면 마지막 배치 결과를 들고 있어야 한다
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    val shape = RoundedCornerShape(Radius.md)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            // 현재 문장은 배경으로만 짚는다. 크기를 키우면 줄 높이가 달라져 레이아웃이
            // 튀고, 그러면 자동 스크롤이 어긋난다
            .background(if (isCurrent) colors.primarySubtle else colors.background)
            .padding(horizontal = Space.x2, vertical = Space.x2),
        verticalArrangement = Arrangement.spacedBy(Space.x1),
    ) {
        if (speakerLabel != null) {
            Text(
                speakerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) colors.primary else colors.textTertiary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
            color = when {
                failed -> colors.textTertiary
                isCurrent -> colors.textPrimary
                else -> colors.textSecondary
            },
            onTextLayout = { layout = it },
            modifier = Modifier.pointerInput(text) {
                detectTapGestures { position ->
                    /*
                     * 탭한 자리를 글자 번호로 바꾼다.
                     *
                     * 탭이 재생 위치 이동이므로 **텍스트 복사는 길게 누르기**로 남긴다
                     * (시스템 선택). 둘을 같은 제스처에 두면 하나를 못 쓴다.
                     */
                    val offset = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                    onTapOffset(offset)
                }
            },
        )
        if (failed) {
            Text(
                "이 문장은 소리를 만들지 못했습니다",
                style = MaterialTheme.typography.labelSmall,
                color = colors.negative,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .padding(horizontal = Space.x4, vertical = Space.x3),
        verticalArrangement = Arrangement.spacedBy(Space.x2),
    ) {
        Slider(
            value = state.positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..maxOf(1f, state.totalMs.toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = AppTheme.colors.primary,
                activeTrackColor = AppTheme.colors.primary,
                inactiveTrackColor = AppTheme.colors.surfaceSunken,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.positionMs.asClock(),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                state.totalMs.asClock(),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 되감기·빨리감기는 글자로 둔다. 초 단위가 숫자로 보여야 얼마나 움직이는지 안다
            RoundButton(description = "10초 뒤로", onClick = onRewind) {
                ControlLabel("−10초")
            }
            RoundButton(description = "이전 문장", onClick = onPreviousSentence) {
                SkipIcon(color = AppTheme.colors.textPrimary, forward = false)
            }
            RoundButton(
                description = if (state.playing) "정지" else "재생",
                onClick = onTogglePlay,
                size = 64.dp,
                filled = true,
            ) {
                if (state.playing) {
                    PauseIcon(color = AppTheme.colors.onPrimary, size = 26.dp)
                } else {
                    PlayIcon(color = AppTheme.colors.onPrimary, size = 26.dp)
                }
            }
            RoundButton(description = "다음 문장", onClick = onNextSentence) {
                SkipIcon(color = AppTheme.colors.textPrimary, forward = true)
            }
            RoundButton(description = "10초 앞으로", onClick = onForward) {
                ControlLabel("+10초")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.x2, Alignment.CenterHorizontally),
        ) {
            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                AppChip(
                    label = speed.asSpeedLabel(),
                    selected = state.speed == speed,
                    onClick = { onSpeed(speed) },
                )
            }
        }
    }
}

/**
 * 둥근 단추.
 *
 * 안에 무엇을 넣을지는 부르는 쪽이 정한다 — 도형이 들어오기도 하고 글자가 들어오기도 한다.
 * 뜻은 [description] 으로 따로 주어, 도형이든 글자든 낭독기가 제대로 읽는다.
 */
@Composable
private fun RoundButton(
    description: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    filled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(Radius.full))
            .background(if (filled) colors.primary else colors.surfaceSunken)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ControlLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = AppTheme.colors.textPrimary,
    )
}

internal fun Long.asClock(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun Float.asSpeedLabel(): String =
    if (this % 1f == 0f) "${toInt()}.0" else "$this"

@Preview
@Composable
private fun PlayerLightPreview() {
    AppTheme(darkTheme = false) {
        PlayerScreen(
            state = previewPlayerState(),
            onBack = {},
            onTogglePlay = {},
            onRewind = {},
            onForward = {},
            onPreviousSentence = {},
            onNextSentence = {},
            onSeek = {},
            onSpeed = {},
            onTapWord = { _, _ -> },
        )
    }
}
