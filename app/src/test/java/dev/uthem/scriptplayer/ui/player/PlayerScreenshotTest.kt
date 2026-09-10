package dev.uthem.scriptplayer.ui.player

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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PlayerScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `재생 중 · 밝은 테마`() = capture("player-light", darkTheme = false, previewPlayerState())

    @Test
    fun `재생 중 · 어두운 테마`() = capture("player-dark", darkTheme = true, previewPlayerState())

    @Test
    fun `정지 · 밝은 테마`() =
        capture("player-paused-light", darkTheme = false, previewPlayerState(playing = false))

    @Test
    fun `합성 중 · 어두운 테마`() =
        capture("player-synthesizing-dark", darkTheme = true, previewPlayerState(synthesizing = true))

    private fun capture(name: String, darkTheme: Boolean, state: PlayerUiState) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) {
                PlayerScreen(
                    state = state,
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
        /*
         * 찍기 전에 본문과 컨트롤이 **둘 다** 그려졌는지 단언한다.
         *
         * 처음에 화자 라벨("진행자")을 앵커로 썼는데 두 문장에 있어 노드가 둘이었다 —
         * 앵커는 고유해야 한다. 지금 문장과 배속 칩은 각각 하나씩만 있다.
         */
        compose.onNodeWithText("맞습니다", substring = true).assertIsDisplayed()
        compose.onNodeWithText("1.25").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
