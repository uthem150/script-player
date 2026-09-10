package dev.uthem.scriptplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 토큰 카탈로그를 두 테마로 찍는다.
 *
 * Robolectric 이 JVM 에서 렌더하므로 기기도 에뮬레이터도 필요 없다.
 * `recordRoborazziDebug` 로 기준 이미지를 만들고 `verifyRoborazziDebug` 로 회귀를 잡는다.
 *
 * 렌더하는 SDK 는 35 로 고정한다. compileSdk 는 37 이지만 Robolectric 은 자기가 가진
 * android-all 로 그리므로, 최신으로 두면 지원하지 않는 SDK 라며 멈춘다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class CatalogScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `색 · 밝은 테마`() = capture("color-light", "색 토큰", darkTheme = false) { ColorCatalog() }

    @Test
    fun `색 · 어두운 테마`() = capture("color-dark", "색 토큰", darkTheme = true) { ColorCatalog() }

    @Test
    fun `글자 · 밝은 테마`() = capture("type-light", "글자 눈금", darkTheme = false) { TypeCatalog() }

    @Test
    fun `글자 · 어두운 테마`() = capture("type-dark", "글자 눈금", darkTheme = true) { TypeCatalog() }

    /**
     * 찍기 전에 내용이 실제로 그려졌는지 단언한다.
     *
     * Compass 의 촬영 하네스는 오류 화면을 런타임에 감지했는데, 그래도 네 번은 깨진 화면을
     * 멀쩡한 줄 알고 넘겼다. 여기서는 단언이 실패하면 테스트가 실패하므로, 빈 화면이나
     * 깨진 화면이 기준 이미지로 기록되는 일이 구조적으로 불가능하다.
     */
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
        // 경로를 직접 적는다. captureRoboImage 에 상대 경로를 주면 roborazzi.outputDir 이
        // 아니라 모듈 루트를 기준으로 삼는다 — 처음에 app/ 바로 아래로 떨어졌다.
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }
}
