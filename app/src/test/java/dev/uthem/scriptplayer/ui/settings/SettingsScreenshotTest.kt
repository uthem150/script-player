package dev.uthem.scriptplayer.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import dev.uthem.scriptplayer.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SettingsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `설정 · 밝은 테마`() = capture("settings-light", darkTheme = false, previewSettings())

    @Test
    fun `설정 · 어두운 테마`() = capture("settings-dark", darkTheme = true, previewSettings())

    @Test
    fun `설정 · 음성 없음`() =
        capture("settings-no-voice", darkTheme = false, previewSettingsWithoutVoices())

    @Test
    fun `저장 공간을 읽기 좋게 적는다`() {
        assertEquals("만들어 둔 소리가 없습니다", 0L.asStorageLabel())
        assertEquals("1MB 미만", (500L * 1024).asStorageLabel())
        assertEquals("약 84MB", (84L * 1024 * 1024).asStorageLabel())
    }

    /** 음성 이름이 길어 칩에 다 넣으면 화면을 넘어간다 — 목소리를 가르는 조각만 뽑는다. */
    @Test
    fun `음성 이름에서 알아볼 조각만 뽑는다`() {
        val voices = previewSettings().voices

        assertEquals(listOf("ism", "kob", "koc", "kod"), voices.map { it.shortName() })
    }

    private fun capture(name: String, darkTheme: Boolean, state: SettingsUiState) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) {
                SettingsScreen(
                    state = state,
                    onBack = {},
                    onTheme = {},
                    onSpeakerVoice = { _, _ -> },
                    onClearCache = {},
                    onInstallVoice = {},
                )
            }
        }
        compose.onNodeWithText("화자 목소리").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
