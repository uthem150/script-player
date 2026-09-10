package dev.uthem.scriptplayer.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.uthem.scriptplayer.data.ScriptSummary
import dev.uthem.scriptplayer.ui.component.AppButton
import dev.uthem.scriptplayer.ui.component.AppCard
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Radius
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 보관함. 상태를 받기만 하고 아무것도 들고 있지 않다.
 *
 * 상태를 밖에서 주므로 스크린샷 테스트가 어떤 상태든 그려 볼 수 있다 — 빈 보관함,
 * 긴 제목, 진도 0·중간·끝. 실제 데이터를 기다려야 하면 그중 무엇도 찍을 수 없다.
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAdd: () -> Unit,
    onRename: (ScriptSummary) -> Unit,
    onDelete: (ScriptSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            // targetSdk 35+ 는 화면 끝까지 그리는 것이 기본이라 인셋을 소비해야 한다
            .safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.x4, vertical = Space.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "보관함",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTheme.colors.textPrimary,
            )
            AppButton(text = "새 대본", onClick = onAdd)
        }

        when {
            // 아직 읽어 오는 중에는 아무 말도 하지 않는다. 여기서 "없습니다" 를 띄우면
            // 켤 때마다 그 문구가 한 번 번쩍인다
            state.loading -> Box(Modifier.fillMaxSize())
            state.scripts.isEmpty() -> EmptyLibrary()
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Space.x4,
                    end = Space.x4,
                    bottom = Space.x8,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.x3),
            ) {
                items(state.scripts, key = { it.id }) { script ->
                    ScriptRow(
                        script = script,
                        onRename = { onRename(script) },
                        onDelete = { onDelete(script) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.x6),
        verticalArrangement = Arrangement.spacedBy(Space.x3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "아직 담긴 대본이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )
        // 무엇을 넣으면 되는지 보여준다. 빈 화면에 단추만 두면 무엇을 붙여넣을지 모른다
        Text(
            "AI 에게 팟캐스트 대본으로 만들어 달라고 한 글을 그대로 붙여넣으세요. " +
                "다른 앱에서 텍스트를 공유해도 들어옵니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun ScriptRow(
    script: ScriptSummary,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        /*
         * 뭇단추를 카드 안에 늘어놓지 않는다.
         *
         * 처음에 «이름 변경»·«삭제» 를 카드마다 두 개씩 뒀는데, 세 장이면 단추가 여섯 개라
         * 제목과 진도보다 단추가 먼저 눈에 들어왔다. 중심은 대본이지 관리 기능이 아니다.
         */
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.x1),
            ) {
                Text(
                    script.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    script.metaLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                )
            }
            MoreMenu(onRename = onRename, onDelete = onDelete)
        }
        ProgressBar(progress = script.progress)
    }
}

@Composable
private fun MoreMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        // 아이콘 자산 없이 IconButton 을 쓴다 — 48dp 터치 영역과 물결 효과는 그대로 얻는다
        IconButton(onClick = { open = true }) {
            Text(
                "⋯",
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.semantics { contentDescription = "대본 관리" },
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = AppTheme.colors.surface,
        ) {
            DropdownMenuItem(
                text = { Text("이름 변경", color = AppTheme.colors.textPrimary) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("삭제", color = AppTheme.colors.negative) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.full)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(AppTheme.colors.surfaceSunken),
    ) {
        // 0 일 때는 아무것도 그리지 않는다. 실선 한 점이 남으면 "조금 들었다" 로 읽힌다
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(AppTheme.colors.primary, shape),
            )
        }
    }
}

/**
 * 카드 둘째 줄.
 *
 * 예상 길이는 문장 수로 어림한다 — 실제 길이는 합성해 봐야 알지만, 목록에서는
 * "십 분쯤" 인지 "한 시간" 인지만 알면 된다. 실측(§12)에서 문장당 평균 4.4초였다.
 */
internal fun ScriptSummary.metaLine(): String {
    val seconds = sentenceCount * 44 / 10
    val minutes = (seconds + 30) / 60
    val length = if (minutes < 1) "1분 미만" else "약 ${minutes}분"
    val percent = (progress * 100).toInt()
    return when {
        percent <= 0 -> "${sentenceCount}문장 · $length"
        percent >= 100 -> "${sentenceCount}문장 · $length · 끝까지 들었음"
        else -> "${sentenceCount}문장 · $length · ${percent}% 들었음"
    }
}

@Preview
@Composable
private fun LibraryPreview() {
    AppTheme(darkTheme = false) {
        LibraryScreen(
            state = LibraryUiState(scripts = previewScripts(), loading = false),
            onAdd = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@Preview
@Composable
private fun EmptyPreview() {
    AppTheme(darkTheme = false) {
        LibraryScreen(
            state = LibraryUiState(loading = false),
            onAdd = {},
            onRename = {},
            onDelete = {},
        )
    }
}

/**
 * 미리보기와 스크린샷이 함께 쓰는 표본.
 *
 * 길이가 제각각인 제목과 진도 0·중간·끝을 섞는다. 다 짧고 다 0% 이면 줄이 넘칠 때와
 * 진도 막대가 찬 모습을 한 번도 못 본다.
 */
internal fun previewScripts(): List<ScriptSummary> = listOf(
    ScriptSummary(
        id = "1",
        title = "자바스크립트 이벤트 루프, 한 번에 이해하기",
        sentenceCount = 41,
        updatedAt = 0,
        lastSentenceIndex = 25,
    ),
    ScriptSummary(
        id = "2",
        title = "안드로이드 렌더링 파이프라인이 프레임을 만드는 과정과 " +
            "왜 16밀리초를 넘기면 화면이 걸려 보이는지",
        sentenceCount = 128,
        updatedAt = 0,
        lastSentenceIndex = 0,
    ),
    ScriptSummary(
        id = "3",
        title = "TCP 혼잡 제어",
        sentenceCount = 12,
        updatedAt = 0,
        lastSentenceIndex = 12,
    ),
)
