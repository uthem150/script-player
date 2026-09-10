package dev.uthem.scriptplayer.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import dev.uthem.scriptplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 보관함 화면을 상태별로 찍는다.
 *
 * 화면이 상태를 받기만 하므로 데이터베이스 없이 어떤 상태든 그려 볼 수 있다.
 * 표본에는 길이가 제각각인 제목과 진도 0·중간·끝을 섞어 둔다 — 다 짧고 다 0% 이면
 * 줄이 넘칠 때와 막대가 찬 모습을 한 번도 못 본다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class LibraryScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `보관함 목록 · 밝은 테마`() = capture("library-light", "보관함", darkTheme = false) {
        LibraryScreen(
            state = LibraryUiState(scripts = previewScripts(), loading = false),
            onAdd = {},
            onOpen = {},
            onRename = {},
            onDelete = {},
        )
    }

    @Test
    fun `보관함 목록 · 어두운 테마`() = capture("library-dark", "보관함", darkTheme = true) {
        LibraryScreen(
            state = LibraryUiState(scripts = previewScripts(), loading = false),
            onAdd = {},
            onOpen = {},
            onRename = {},
            onDelete = {},
        )
    }

    @Test
    fun `빈 보관함 · 밝은 테마`() =
        capture("library-empty-light", "아직 담긴 대본이 없습니다", darkTheme = false) {
            LibraryScreen(
                state = LibraryUiState(loading = false),
                onAdd = {},
                onOpen = {},
                onRename = {},
                onDelete = {},
            )
        }

    @Test
    fun `빈 보관함 · 어두운 테마`() =
        capture("library-empty-dark", "아직 담긴 대본이 없습니다", darkTheme = true) {
            LibraryScreen(
                state = LibraryUiState(loading = false),
                onAdd = {},
                onOpen = {},
                onRename = {},
                onDelete = {},
            )
        }

    @Test
    fun `새 대본 · 밝은 테마`() = capture("add-script-light", "새 대본", darkTheme = false) {
        AddScriptScreen(text = "", onTextChange = {}, onSave = {}, onCancel = {})
    }

    @Test
    fun `새 대본 · 어두운 테마`() = capture("add-script-dark", "새 대본", darkTheme = true) {
        AddScriptScreen(text = "", onTextChange = {}, onSave = {}, onCancel = {})
    }

    @Test
    fun `읽을 문장 없음 · 밝은 테마`() =
        capture("add-error-light", "새 대본", darkTheme = false) { CodeOnlyPaste() }

    @Test
    fun `읽을 문장 없음 · 어두운 테마`() =
        capture("add-error-dark", "새 대본", darkTheme = true) { CodeOnlyPaste() }

    /** 코드 블록만 붙여넣어 읽을 것이 남지 않은 상태. */
    @Composable
    private fun CodeOnlyPaste() {
        AddScriptScreen(
            text = "```javascript\nconsole.log('1')\nsetTimeout(() => console.log('2'), 0)\n```",
            onTextChange = {},
            onSave = {},
            onCancel = {},
            error = "읽을 문장이 없습니다. 코드 블록과 표는 소리로 읽지 않습니다.",
        )
    }

    /** 찍기 전에 내용이 그려졌는지 단언한다 — 깨진 화면이 기준 이미지로 남는 것을 막는다. */
    private fun capture(
        name: String,
        anchor: String,
        darkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) { content() }
        }
        compose.onNodeWithText(anchor).assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
