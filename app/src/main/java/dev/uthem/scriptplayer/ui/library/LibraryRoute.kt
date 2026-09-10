package dev.uthem.scriptplayer.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.uthem.scriptplayer.appContainer
import dev.uthem.scriptplayer.data.AddResult
import dev.uthem.scriptplayer.data.ScriptSummary
import kotlinx.coroutines.launch
import dev.uthem.scriptplayer.ui.component.AppButton
import dev.uthem.scriptplayer.ui.component.AppOutlinedButton
import dev.uthem.scriptplayer.ui.component.AppTextField
import dev.uthem.scriptplayer.ui.theme.AppTheme

/**
 * 보관함에 상태를 붙이는 층.
 *
 * 화면([LibraryScreen])은 상태를 받기만 한다. 이 둘을 나눠 두면 스크린샷 테스트가
 * 데이터베이스 없이 어떤 상태든 그려 볼 수 있다.
 */
@Composable
fun LibraryRoute(
    onOpenAdd: () -> Unit,
    onOpenScript: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val viewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LibraryViewModel(container.scripts) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var renaming by remember { mutableStateOf<ScriptSummary?>(null) }
    var deleting by remember { mutableStateOf<ScriptSummary?>(null) }

    LibraryScreen(
        state = state,
        onAdd = onOpenAdd,
        onOpenSettings = onOpenSettings,
        onOpen = { onOpenScript(it.id) },
        onRename = { renaming = it },
        onDelete = { deleting = it },
        modifier = modifier,
    )

    renaming?.let { target ->
        RenameDialog(
            current = target.title,
            onConfirm = { title ->
                viewModel.rename(target.id, title)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { target ->
        DeleteDialog(
            title = target.title,
            onConfirm = {
                viewModel.delete(target.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

/**
 * 새 대본 붙여넣기에 상태를 붙이는 층.
 *
 * 붙여넣은 글을 [rememberSaveable] 로 들고 있다. 화면을 돌리거나 잠깐 다른 앱에 갔다
 * 왔을 때 붙여넣은 대본이 사라지면, 다시 찾아 복사해 와야 한다.
 */
@Composable
fun AddScriptRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AddScriptScreen(
        text = text,
        onTextChange = {
            text = it
            error = null
        },
        onSave = {
            scope.launch {
                when (container.scripts.add(text)) {
                    is AddResult.Added -> onDone()
                    AddResult.NothingToRead ->
                        error = "읽을 문장이 없습니다. 코드 블록과 표는 소리로 읽지 않습니다."
                }
            }
        },
        onCancel = onDone,
        error = error,
        modifier = modifier,
    )
}

@Composable
private fun RenameDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = {
            Text("이름 변경", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
        },
        text = {
            AppTextField(value = text, onValueChange = { text = it }, placeholder = "제목")
        },
        confirmButton = {
            AppButton(text = "바꾸기", onClick = { onConfirm(text) }, enabled = text.isNotBlank())
        },
        dismissButton = {
            AppOutlinedButton(text = "취소", onClick = onDismiss)
        },
    )
}

@Composable
private fun DeleteDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = {
            Text("삭제할까요?", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
        },
        text = {
            // 무엇을 지우는지 제목으로 확인시킨다. 목록에서 잘못 누르는 일이 흔하다
            Text(
                "\"$title\" 을 지웁니다. 되돌릴 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        },
        confirmButton = {
            AppButton(text = "삭제", onClick = onConfirm)
        },
        dismissButton = {
            AppOutlinedButton(text = "취소", onClick = onDismiss)
        },
    )
}
