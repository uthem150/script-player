package dev.uthem.scriptplayer.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 음성 고르기와 화자 배정.
 *
 * 순수 함수라 기기 없이 돈다. 이 필터가 새면 "인터넷 없이 돈다" 는 약속이 깨진다.
 */
class VoiceSelectionTest {

    private fun voice(
        name: String,
        language: String = "ko",
        quality: Int = 400,
        needsNetwork: Boolean = false,
        notInstalled: Boolean = false,
    ) = VoiceInfo(name, language, quality, needsNetwork, notInstalled)

    /**
     * 실측 기기의 실제 목록이다.
     *
     * 같은 화자가 `-local` 과 `-network` 로 짝지어 있다. 이름만 보고 고르면 네트워크
     * 음성을 집어 비행기 모드에서 조용히 실패한다.
     */
    private val realDeviceVoices = listOf(
        voice("ko-KR-language"),
        voice("ko-kr-x-ism-local"),
        voice("ko-kr-x-ism-network", needsNetwork = true),
        voice("ko-kr-x-kob-local"),
        voice("ko-kr-x-kob-network", needsNetwork = true),
        voice("ko-kr-x-koc-local"),
        voice("ko-kr-x-koc-network", needsNetwork = true),
        voice("ko-kr-x-kod-local"),
        voice("ko-kr-x-kod-network", needsNetwork = true),
        voice("kok-IN-language", language = "kok", needsNetwork = true),
        voice("en-us-x-sfg-local", language = "en"),
    )

    @Test
    fun `네트워크가 필요한 음성을 걸러낸다`() {
        val picked = realDeviceVoices.offlineKorean()

        assertTrue(
            "네트워크 음성이 남았다: ${picked.map { it.name }}",
            picked.none { it.needsNetwork },
        )
        assertEquals(5, picked.size)
    }

    /** `kok` (콘칸어) 은 `ko` 로 시작하지만 한국어가 아니다. */
    @Test
    fun `언어 코드가 앞부분만 같은 것을 고르지 않는다`() {
        val picked = realDeviceVoices.offlineKorean()

        assertTrue(
            "콘칸어가 섞였다: ${picked.map { it.name }}",
            picked.none { it.name.startsWith("kok") },
        )
    }

    @Test
    fun `설치되지 않은 음성을 걸러낸다`() {
        val picked = listOf(
            voice("ko-a"),
            voice("ko-b", notInstalled = true),
        ).offlineKorean()

        assertEquals(listOf("ko-a"), picked.map { it.name })
    }

    @Test
    fun `품질이 높은 것이 앞에 온다`() {
        val picked = listOf(
            voice("ko-low", quality = 100),
            voice("ko-high", quality = 500),
            voice("ko-mid", quality = 300),
        ).offlineKorean()

        assertEquals(listOf("ko-high", "ko-mid", "ko-low"), picked.map { it.name })
    }

    @Test
    fun `쓸 수 있는 한국어 음성이 없으면 빈 목록이다`() {
        val picked = listOf(
            voice("ko-only-network", needsNetwork = true),
            voice("en-local", language = "en"),
        ).offlineKorean()

        assertTrue(picked.isEmpty())
    }

    @Test
    fun `화자마다 다른 목소리를 준다`() {
        val voices = listOf(voice("v1"), voice("v2"), voice("v3"), voice("v4"))

        val assigned = assignVoices(listOf("s0", "s1"), voices)

        assertEquals("v1", assigned["s0"]?.voiceName)
        assertEquals("v2", assigned["s1"]?.voiceName)
        assertEquals(1.0f, assigned["s0"]?.pitch)
        assertEquals(1.0f, assigned["s1"]?.pitch)
    }

    /**
     * 화자가 음성보다 많으면 음성을 돌려 쓰면서 음높이로 갈라 준다.
     * 같은 목소리 같은 높이로 두 사람을 읽으면 대담이 혼잣말처럼 들린다.
     */
    @Test
    fun `화자가 음성보다 많으면 음높이로 갈라 준다`() {
        val voices = listOf(voice("v1"), voice("v2"))

        val assigned = assignVoices(listOf("s0", "s1", "s2", "s3"), voices)

        assertEquals("v1", assigned["s0"]?.voiceName)
        assertEquals("v1", assigned["s2"]?.voiceName)
        // 같은 목소리를 다시 쓸 때는 음높이가 달라야 한다
        assertTrue(
            "같은 목소리에 같은 높이를 줬다",
            assigned["s0"]?.pitch != assigned["s2"]?.pitch,
        )
    }

    @Test
    fun `음성이 하나뿐이면 음높이만으로 갈라 준다`() {
        val assigned = assignVoices(listOf("s0", "s1", "s2"), listOf(voice("only")))

        assertTrue("모두 같은 목소리여야 한다", assigned.values.all { it.voiceName == "only" })
        assertEquals("높이가 셋 다 달라야 한다", 3, assigned.values.map { it.pitch }.distinct().size)
    }

    @Test
    fun `쓸 음성이 없으면 배정하지 않는다`() {
        assertTrue(assignVoices(listOf("s0"), emptyList()).isEmpty())
    }
}
