package dev.uthem.scriptplayer.ui.share

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.ui.component.AppButton
import dev.uthem.scriptplayer.ui.component.AppCard
import dev.uthem.scriptplayer.ui.component.AppOutlinedButton
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 공유받은 글을 담을지 확인한다.
 *
 * 바로 담지 않는 이유는 잘못 던진 글이 조용히 쌓이는 것을 막기 위해서다.
 * 무엇이 들어왔는지 제목·화자·앞 문장으로 보여주고 사용자가 정한다.
 */
@Composable
fun ShareConfirmScreen(
    preview: SharePreview,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
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
            "받은 글을 담을까요?",
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
        )

        if (!preview.hasSomethingToRead) {
            Text(
                "읽을 문장이 없습니다. 코드 블록과 표는 소리로 읽지 않으니, 설명하는 " +
                    "문장이 담긴 글을 공유해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        } else {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    preview.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    preview.summaryLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                )
            }

            Text(
                "이렇게 읽습니다",
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textSecondary,
            )
            // 앞 문장을 보여준다. 제목만으로는 엉뚱한 글을 던졌는지 알 수 없다
            preview.opening.forEach { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Space.x2),
            horizontalArrangement = Arrangement.spacedBy(Space.x2, Alignment.End),
        ) {
            AppOutlinedButton(text = "취소", onClick = onCancel)
            if (preview.hasSomethingToRead) {
                AppButton(text = "담기", onClick = onSave)
            }
        }
    }
}

@Preview
@Composable
private fun ShareConfirmPreview() {
    AppTheme(darkTheme = false) {
        ShareConfirmScreen(preview = previewShare(), onSave = {}, onCancel = {})
    }
}

@Preview
@Composable
private fun ShareEmptyPreview() {
    AppTheme(darkTheme = false) {
        ShareConfirmScreen(
            preview = sharePreviewOf("```\nconsole.log('a')\n```"),
            onSave = {},
            onCancel = {},
        )
    }
}

/** 미리보기와 스크린샷이 함께 쓰는 표본 — 실제 대본 모양 그대로. */
internal fun previewShare(): SharePreview = sharePreviewOf(
    """
    # 자바스크립트 이벤트 루프, 한 번에 이해하기

    **진행자**: 안녕하세요. 오늘은 자바스크립트의 이벤트 루프를 다뤄보겠습니다.

    **게스트**: 맞습니다. 자바스크립트는 한 번에 하나의 일만 처리합니다.

    **진행자**: 그러면 실제로 코드가 실행될 때 어떤 순서로 움직이는 건가요?

    **게스트**: 세 개의 공간을 떠올려보세요. 콜 스택, 태스크 큐, 마이크로태스크 큐입니다.
    """.trimIndent(),
)
