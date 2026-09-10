package dev.uthem.scriptplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 진도 누적합.
 *
 * 진도 막대가 뒤로 튀는지는 눈으로 확인하기 어렵고, 튀면 사용자는 재생기가 고장난 줄 안다.
 * 그래서 여기를 숫자로 못 박는다.
 */
class SentenceTimelineTest {

    @Test
    fun `합성 전에는 글자 수로 어림한다`() {
        val timeline = SentenceTimeline(listOf(10, 20))

        assertEquals(SentenceTimeline.estimate(10), timeline.durationOf(0))
        assertFalse(timeline.isMeasured(0))
        assertTrue(timeline.totalMs > 0)
    }

    @Test
    fun `실측이 들어오면 바꿔 넣는다`() {
        val timeline = SentenceTimeline(listOf(10, 20))

        timeline.measure(0, 5_000)

        assertEquals(5_000, timeline.durationOf(0))
        assertTrue(timeline.isMeasured(0))
        assertFalse("아직 안 온 것은 어림값이어야 한다", timeline.isMeasured(1))
    }

    @Test
    fun `잘못된 실측은 무시한다`() {
        val timeline = SentenceTimeline(listOf(10))
        val before = timeline.durationOf(0)

        timeline.measure(0, 0)
        timeline.measure(0, -100)
        timeline.measure(5, 9_999)

        assertEquals(before, timeline.durationOf(0))
    }

    /**
     * 어림값은 실제보다 작게 잡는다.
     *
     * 크게 잡으면 실측이 들어올 때마다 전체 길이가 줄어, 같은 위치인데 진도 비율이
     * 커진다 — 막대가 앞으로 튄다. 짧게 잡으면 늘어나는 쪽이라 덜 거슬린다.
     *
     * 실측 기준 글자당 약 138ms 였다.
     */
    @Test
    fun `어림값은 실제 속도보다 짧게 잡는다`() {
        assertTrue(
            "어림값이 실측(글자당 138ms)보다 크면 진도가 앞으로 튄다",
            SentenceTimeline.estimate(100) < 100 * 138L,
        )
    }

    @Test
    fun `아주 짧은 문장도 0 이 아니다`() {
        // 0 이면 그 문장이 진도에서 사라져, 재생 중인데 막대가 멈춘 것처럼 보인다
        assertTrue(SentenceTimeline.estimate(0) > 0)
        assertTrue(SentenceTimeline.estimate(1) > 0)
    }

    @Test
    fun `문장 시작 시각은 앞 문장들의 합이다`() {
        val timeline = SentenceTimeline(listOf(10, 10, 10))
        timeline.measure(0, 1_000)
        timeline.measure(1, 2_000)
        timeline.measure(2, 3_000)

        assertEquals(0, timeline.startOf(0))
        assertEquals(1_000, timeline.startOf(1))
        assertEquals(3_000, timeline.startOf(2))
        assertEquals(6_000, timeline.totalMs)
    }

    @Test
    fun `전체 위치는 시작 시각에 문장 안 위치를 더한 값이다`() {
        val timeline = SentenceTimeline(listOf(10, 10))
        timeline.measure(0, 1_000)
        timeline.measure(1, 2_000)

        assertEquals(1_500, timeline.positionOf(1, 500))
    }

    @Test
    fun `문장 길이를 넘는 위치는 그 문장 끝으로 자른다`() {
        val timeline = SentenceTimeline(listOf(10))
        timeline.measure(0, 1_000)

        assertEquals(1_000, timeline.positionOf(0, 9_999))
        assertEquals(0, timeline.positionOf(0, -500))
    }

    @Test
    fun `전체 시각을 문장과 그 안 위치로 되돌린다`() {
        val timeline = SentenceTimeline(listOf(10, 10, 10))
        timeline.measure(0, 1_000)
        timeline.measure(1, 2_000)
        timeline.measure(2, 3_000)

        assertEquals(SentenceTimeline.Location(0, 0), timeline.locate(0))
        assertEquals(SentenceTimeline.Location(0, 999), timeline.locate(999))
        assertEquals(SentenceTimeline.Location(1, 0), timeline.locate(1_000))
        assertEquals(SentenceTimeline.Location(2, 500), timeline.locate(3_500))
    }

    @Test
    fun `끝을 넘어가면 마지막 문장 끝으로 둔다`() {
        val timeline = SentenceTimeline(listOf(10, 10))
        timeline.measure(0, 1_000)
        timeline.measure(1, 1_000)

        assertEquals(SentenceTimeline.Location(1, 1_000), timeline.locate(99_999))
    }

    @Test
    fun `음수 위치는 처음으로 둔다`() {
        val timeline = SentenceTimeline(listOf(10))

        assertEquals(SentenceTimeline.Location(0, 0), timeline.locate(-5_000))
    }

    @Test
    fun `문장이 없으면 처음을 가리킨다`() {
        val timeline = SentenceTimeline(emptyList())

        assertEquals(0, timeline.totalMs)
        assertEquals(SentenceTimeline.Location(0, 0), timeline.locate(1_000))
    }

    /**
     * 실측이 하나씩 들어오는 동안 같은 자리의 진도가 뒤로 가지 않아야 한다.
     *
     * 어림값이 실제보다 짧으므로 전체 길이는 늘어난다. 그러면 같은 절대 위치의 비율은
     * 줄어드는데, 그것은 "뒤로 튀는" 것이다. 그래서 **비율이 아니라 절대 위치**로
     * 다룬다는 것을 여기서 못 박는다 — 화면은 늘 지금의 totalMs 로 다시 계산한다.
     */
    @Test
    fun `실측이 들어와도 앞 문장의 시작 시각은 변하지 않는다`() {
        val timeline = SentenceTimeline(listOf(10, 10, 10))
        timeline.measure(0, 2_000)
        val startOfSecond = timeline.startOf(1)

        // 뒤 문장의 실측이 들어와도 앞 문장의 자리는 그대로다
        timeline.measure(2, 9_000)

        assertEquals(startOfSecond, timeline.startOf(1))
    }
}
