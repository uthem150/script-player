package dev.uthem.scriptplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import dev.uthem.scriptplayer.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 앱 아이콘을 눈으로 확인한다.
 *
 * 벡터로 그린 것은 코드만 봐서는 어떻게 보이는지 알 수 없고, 특히 **작을 때 뭉개지는지**는
 * 반드시 그려 봐야 한다. 런처가 씌우는 모양(원·둥근 사각)과 실제로 쓰이는 크기들을
 * 한 장에 담아 둔다.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class IconScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private companion object {
        const val CAPTURE = "icon-sheet"
    }

    @Test
    fun `앱 아이콘 · 모양과 크기별`() {
        compose.setContent {
            // 화면 전체가 아니라 이 덩어리만 찍는다 — 리드미에 넣을 그림이라
            // 아래쪽 빈 공간이 남으면 안 된다.
            Column(
                modifier = Modifier
                    .testTag(CAPTURE)
                    .fillMaxWidth()
                    .background(Color(0xFFF2F4F6))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text("적응형 아이콘 — 런처가 씌우는 모양")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherIcon(size = 96.dp, shape = CircleShape)
                    LauncherIcon(size = 96.dp, shape = RoundedCornerShape(24.dp))
                    LauncherIcon(size = 96.dp, shape = RoundedCornerShape(48.dp))
                }

                Text("실제로 쓰이는 크기 — 작을 때 뭉개지지 않아야 한다")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherIcon(size = 64.dp, shape = CircleShape)
                    LauncherIcon(size = 48.dp, shape = CircleShape)
                    LauncherIcon(size = 36.dp, shape = CircleShape)
                    LauncherIcon(size = 24.dp, shape = CircleShape)
                }
            }
        }
        compose.onNodeWithTag(CAPTURE).captureRoboImage("screenshots/app-icon.png")
    }

    /**
     * 런처가 하는 일을 흉내낸다.
     *
     * 적응형 아이콘은 108 짜리 그림을 그리고 가운데 72 만 보여준다 — 그래서 그림을
     * 1.5배로 키우고 모양대로 깎는다. 이렇게 해야 잘리는 자리가 실제와 같다.
     */
    @Composable
    private fun LauncherIcon(size: Dp, shape: Shape) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(Color(0xFF3182F6)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(size * 1.5f),
            )
        }
    }
}
