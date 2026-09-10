package dev.uthem.scriptplayer.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 3계층 토큰이 무너지지 않게 지키는 테스트.
 *
 * 색은 눈으로 봐야 하는 것이라 스크린샷 테스트가 본진이지만, 여기서 잡히는 종류가 따로 있다.
 * 한쪽 테마만 고치고 지나가는 실수와, 화면이 계층을 건너뛰어 원시 값을 직접 쓰는 것이다.
 */
class TokenTest {

    private val light = LightAliases.toAppColors()
    private val dark = DarkAliases.toAppColors()

    /**
     * 두 테마가 실제로 다른지 본다.
     *
     * Compass 에서 밝은 값과 어두운 값을 두 벌로 나눠 적었다가 한쪽만 고치는 일이 실제로
     * 벌어졌다. 어두운 테마를 쓰는 사람은 고쳐지지 않은 색을 봤다.
     */
    @Test
    fun `밝은 테마와 어두운 테마는 바탕·글자·강조가 서로 다르다`() {
        val roles = listOf(
            "background" to (light.background to dark.background),
            "surface" to (light.surface to dark.surface),
            "surfaceSunken" to (light.surfaceSunken to dark.surfaceSunken),
            "textPrimary" to (light.textPrimary to dark.textPrimary),
            "textSecondary" to (light.textSecondary to dark.textSecondary),
            "border" to (light.border to dark.border),
            "primary" to (light.primary to dark.primary),
            "onPrimary" to (light.onPrimary to dark.onPrimary),
            "primarySubtle" to (light.primarySubtle to dark.primarySubtle),
        )
        val same = roles.filter { (_, pair) -> pair.first == pair.second }.map { it.first }
        assertEquals("두 테마에서 같은 값을 쓰는 역할", emptyList<String>(), same)
    }

    /** 상태 색은 두 테마에서 같아도 된다 — 빨강이 위험이라는 뜻은 테마와 무관하다. */
    @Test
    fun `상태 색은 두 테마에서 같다`() {
        assertEquals(light.positive, dark.positive)
        assertEquals(light.negative, dark.negative)
        assertEquals(light.warning, dark.warning)
    }

    @Test
    fun `모든 역할 색이 불투명하다`() {
        val transparent = (light.roles() + dark.roles())
            .filter { (_, color) -> color.alpha < 1f }
            .map { it.first }
        assertEquals("반투명하게 남은 역할", emptyList<String>(), transparent)
    }

    /**
     * 화면이 계층을 건너뛰지 않는지 본다.
     *
     * `Primitive.Blue500` 을 화면에서 직접 쓰면 어두운 테마에서 그 자리만 안 바뀐다.
     * 눈으로는 밝은 화면만 보고 지나치기 쉬워, 소스에서 잡는다.
     */
    @Test
    fun `theme 밖의 화면은 프리미티브와 별칭을 직접 참조하지 않는다`() {
        val uiRoot = File("src/main/java/dev/uthem/scriptplayer/ui")
        assertTrue("경로가 바뀌었다: ${uiRoot.absolutePath}", uiRoot.isDirectory)

        val offenders = uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.parentFile?.name == "theme" }
            .filter { file ->
                val text = file.readText()
                text.contains("Primitive.") || text.contains("Aliases")
            }
            .map { it.name }
            .sorted()
            .toList()

        assertEquals("3계층을 건너뛴 파일", emptyList<String>(), offenders)
    }
}

/**
 * 역할과 색을 짝지어 낸다.
 *
 * 리플렉션을 쓰지 않으려고 손으로 적는다 — 역할을 새로 추가하면 여기에도 넣어야 한다.
 */
private fun AppColors.roles(): List<Pair<String, Color>> = listOf(
    "background" to background,
    "surface" to surface,
    "surfaceHover" to surfaceHover,
    "surfaceSunken" to surfaceSunken,
    "textPrimary" to textPrimary,
    "textSecondary" to textSecondary,
    "textTertiary" to textTertiary,
    "border" to border,
    "borderStrong" to borderStrong,
    "primary" to primary,
    "primaryHover" to primaryHover,
    "onPrimary" to onPrimary,
    "primarySubtle" to primarySubtle,
    "positive" to positive,
    "negative" to negative,
    "warning" to warning,
)
