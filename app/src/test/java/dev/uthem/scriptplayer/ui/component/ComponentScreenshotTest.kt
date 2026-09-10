package dev.uthem.scriptplayer.ui.component

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
class ComponentScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `컴포넌트 · 밝은 테마`() = capture("component-light", darkTheme = false)

    @Test
    fun `컴포넌트 · 어두운 테마`() = capture("component-dark", darkTheme = true)

    private fun capture(name: String, darkTheme: Boolean) {
        compose.setContent {
            AppTheme(darkTheme = darkTheme) { ComponentCatalog() }
        }
        // 찍기 전에 내용이 그려졌는지 단언한다 — 깨진 화면이 기준 이미지로 남는 것을 막는다
        compose.onNodeWithText("컴포넌트").assertIsDisplayed()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
