package dev.uthem.scriptplayer.ui.voice

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
class NoVoiceScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `목소리 없음 · 밝은 테마`() = capture("no-voice-light", darkTheme = false)

    @Test
    fun `목소리 없음 · 어두운 테마`() = capture("no-voice-dark", darkTheme = true)

    private fun capture(name: String, darkTheme: Boolean) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) {
                NoVoiceScreen(onOpenSettings = {}, onRetry = {})
            }
        }
        compose.onNodeWithText("읽어 줄 목소리가 없습니다").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
