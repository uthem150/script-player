package dev.uthem.scriptplayer.ui.share

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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ShareScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `공유 확인 · 밝은 테마`() = capture("share-light", darkTheme = false) {
        ShareConfirmScreen(preview = previewShare(), onSave = {}, onCancel = {})
    }

    @Test
    fun `공유 확인 · 어두운 테마`() = capture("share-dark", darkTheme = true) {
        ShareConfirmScreen(preview = previewShare(), onSave = {}, onCancel = {})
    }

    @Test
    fun `공유 확인 · 읽을 것 없음`() = capture("share-empty", darkTheme = false) {
        ShareConfirmScreen(
            preview = sharePreviewOf("```\nconsole.log('a')\n```"),
            onSave = {},
            onCancel = {},
        )
    }

    private fun capture(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) { content() }
        }
        compose.onNodeWithText("받은 글을 담을까요?").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
