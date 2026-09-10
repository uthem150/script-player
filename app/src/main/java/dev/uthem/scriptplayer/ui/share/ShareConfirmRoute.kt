package dev.uthem.scriptplayer.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.uthem.scriptplayer.appContainer
import kotlinx.coroutines.launch

/**
 * 공유받은 글을 담는 층.
 *
 * 미리보기는 [remember] 로 한 번만 만든다 — 다시 그릴 때마다 파싱하면 긴 대본에서
 * 화면이 걸린다.
 */
@Composable
fun ShareConfirmRoute(
    raw: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    val preview = remember(raw) { sharePreviewOf(raw) }
    var saving by remember { mutableStateOf(false) }

    ShareConfirmScreen(
        preview = preview,
        onSave = {
            // 두 번 눌러 두 번 담기는 것을 막는다
            if (saving) return@ShareConfirmScreen
            saving = true
            scope.launch {
                container.scripts.add(raw)
                onDone()
            }
        },
        onCancel = onDone,
        modifier = modifier,
    )
}
