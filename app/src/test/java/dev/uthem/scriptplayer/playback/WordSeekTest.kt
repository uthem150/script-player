package dev.uthem.scriptplayer.playback

import dev.uthem.scriptplayer.tts.WordTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 단어를 탭해 그 지점부터 듣기.
 *
 * 화면으로는 몇 밀리초 어긋났는지 확인할 수 없어 숫자로 못 박는다.
 */
class WordSeekTest {

    // "안녕하세요 오늘은 이벤트 루프입니다" 를 흉내낸 표
    private val timings = listOf(
        WordTiming(charStart = 0, charEnd = 5, atMs = 0),
        WordTiming(charStart = 6, charEnd = 9, atMs = 800),
        WordTiming(charStart = 10, charEnd = 13, atMs = 1_500),
        WordTiming(charStart = 14, charEnd = 19, atMs = 2_200),
    )

    private fun seek(offset: Int) =
        positionForCharOffset(offset, sentenceLength = 19, durationMs = 3_000, timings = timings)

    @Test
    fun `단어 시작을 탭하면 그 단어 시각으로 간다`() {
        assertEquals(0, seek(0))
        assertEquals(800, seek(6))
        assertEquals(1_500, seek(10))
        assertEquals(2_200, seek(14))
    }

    /**
     * 단어 중간을 눌렀을 때 그 단어의 처음부터 읽는 것이 자연스럽다.
     * 다음 단어로 넘기면 방금 누른 낱말을 건너뛰어, 누른 것과 들리는 것이 어긋난다.
     */
    @Test
    fun `단어 중간을 탭하면 그 단어의 처음으로 간다`() {
        assertEquals(800, seek(7))
        assertEquals(800, seek(8))
        assertEquals(1_500, seek(12))
    }

    @Test
    fun `첫 단어보다 앞을 탭하면 처음으로 간다`() {
        assertEquals(0, seek(-5))
    }

    @Test
    fun `문장 끝을 넘겨 탭해도 마지막 단어로 간다`() {
        assertEquals(2_200, seek(999))
    }

    @Test
    fun `타이밍이 없으면 글자 비율로 어림한다`() {
        val position = positionForCharOffset(
            charOffset = 50,
            sentenceLength = 100,
            durationMs = 4_000,
            timings = emptyList(),
        )

        assertEquals(2_000, position)
    }

    @Test
    fun `타이밍이 없고 글자도 없으면 처음이다`() {
        assertEquals(
            0,
            positionForCharOffset(0, sentenceLength = 0, durationMs = 4_000, timings = emptyList()),
        )
    }

    @Test
    fun `길이를 모르면 처음이다`() {
        assertEquals(0, positionForCharOffset(10, sentenceLength = 20, durationMs = 0, timings = timings))
    }

    @Test
    fun `어림값도 문장 길이를 넘지 않는다`() {
        val position = positionForCharOffset(
            charOffset = 500,
            sentenceLength = 100,
            durationMs = 4_000,
            timings = emptyList(),
        )

        assertEquals(4_000, position)
    }

    @Test
    fun `지금 읽는 단어를 짚는다`() {
        assertEquals(0 until 5, currentWordRange(0, timings))
        assertEquals(0 until 5, currentWordRange(799, timings))
        assertEquals(6 until 9, currentWordRange(800, timings))
        assertEquals(14 until 19, currentWordRange(9_999, timings))
    }

    /**
     * 어림값으로 단어를 짚으면 실제로 읽는 것과 어긋나 보인다.
     * 없는 것보다 어수선하므로 단어 하이라이트를 끈다.
     */
    @Test
    fun `타이밍이 없으면 단어를 짚지 않는다`() {
        assertNull(currentWordRange(1_000, emptyList()))
    }

    @Test
    fun `첫 단어 시각보다 앞이면 짚지 않는다`() {
        val late = listOf(WordTiming(0, 3, atMs = 500))

        assertNull(currentWordRange(100, late))
    }
}
