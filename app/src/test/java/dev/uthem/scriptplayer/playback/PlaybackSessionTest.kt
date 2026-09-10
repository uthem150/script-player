package dev.uthem.scriptplayer.playback

import dev.uthem.scriptplayer.parser.parseScript
import dev.uthem.scriptplayer.tts.SynthesisProgress
import dev.uthem.scriptplayer.tts.SynthesizedSentence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 합성과 재생을 잇는 조율.
 *
 * 가짜 재생기로 돈다. 실제 소리는 기기에서만 확인할 수 있지만 **언제 무엇을 부르는지**는
 * 여기서 못 박을 수 있고, 버그는 대개 그쪽에 있다 — 순서가 어긋나면 소리가 뒤죽박죽 난다.
 */
class PlaybackSessionTest {

    private class FakePlayer : PlayerController {
        val appended = mutableListOf<Int>()
        val appendedTexts = mutableListOf<String>()
        val seeks = mutableListOf<Pair<Int, Long>>()
        var speeds = mutableListOf<Float>()
        var capturedTitle: String? = null
        var cleared = 0
        override var currentIndex = 0
        override var positionMs = 0L
        override var isPlaying = false
        override val itemCount: Int get() = appended.size

        override fun setScriptTitle(title: String) {
            capturedTitle = title
        }

        override fun append(index: Int, audio: File, sentenceText: String) {
            appended += index
            appendedTexts += sentenceText
        }

        override fun play() {
            isPlaying = true
        }

        override fun pause() {
            isPlaying = false
        }

        override fun seekTo(itemIndex: Int, withinMs: Long) {
            seeks += itemIndex to withinMs
            currentIndex = itemIndex
            positionMs = withinMs
        }

        override fun setSpeed(speed: Float) {
            speeds += speed
        }

        override fun setPitch(pitch: Float) = Unit

        override fun clear() {
            cleared++
            appended.clear()
        }
    }

    private fun done(index: Int) =
        SynthesisProgress.Done(index, SynthesizedSentence(File("/tmp/$index.wav"), emptyList()))

    private val script = parseScript("하나. 둘. 셋. 넷.")

    @Test
    fun `대본을 걸면 재생기를 비우고 문장 수를 잡는다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)

        session.load(script)

        assertEquals(1, player.cleared)
        assertEquals(4, session.state.value.sentenceCount)
        assertTrue("어림 길이가 잡혀야 한다", session.state.value.totalMs > 0)
    }

    /**
     * 실기기에서 나온 불만.
     *
     * 처음에는 첫 문장이 붙으면 바로 틀었다. 그런데 대본을 열어보려고 카드를 누른
     * 사람에게 갑자기 소리가 터졌다 — 시작은 사용자가 정해야 한다.
     */
    @Test
    fun `대본을 열어도 스스로 재생하지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(flowOf(done(0), done(1), done(2), done(3)))

        assertFalse("누르지 않았는데 재생됐다", player.isPlaying)
        assertEquals(listOf(0, 1, 2, 3), player.appended)
    }

    @Test
    fun `재생을 누르면 그때 시작한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        session.consume(flowOf(done(0)))

        session.play()

        assertTrue(player.isPlaying)
    }

    /**
     * 합성 결과가 뒤섞여 와도 소리는 순서대로여야 한다.
     *
     * 지금 구현은 순서대로 내지만, 나중에 여러 문장을 나란히 합성하면 뒤섞인다.
     * 그때 소리가 뒤죽박죽 나는 것은 찾기 어려운 버그라 처음부터 막아 둔다.
     */
    @Test
    fun `뒤섞여 들어와도 순서대로 붙인다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(flowOf(done(2), done(0), done(3), done(1)))

        assertEquals(listOf(0, 1, 2, 3), player.appended)
    }

    @Test
    fun `가운데가 아직 안 오면 거기서 멈춘다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(flowOf(done(0), done(2), done(3)))

        // 1번이 없으므로 0번까지만 붙는다 — 2·3번을 먼저 붙이면 순서가 어긋난다
        assertEquals(listOf(0), player.appended)
        assertEquals(1, session.state.value.readySentences)
    }

    /**
     * 실패한 문장 때문에 뒤가 영원히 기다리면 안 된다.
     *
     * 순서대로만 붙이므로 자리를 비워 두면 뒤 문장이 절대 붙지 않는다 — 한 문장이
     * 실패했다는 이유로 나머지를 못 듣게 된다.
     */
    @Test
    fun `실패한 문장을 건너뛰고 뒤를 이어 붙인다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(
            flowOf(
                done(0),
                SynthesisProgress.Failed(1, "일부러 실패"),
                done(2),
                done(3),
            ),
        )

        assertEquals(listOf(0, 2, 3), player.appended)
        assertEquals(setOf(1), session.state.value.failedSentences)
    }

    @Test
    fun `실패가 먼저 와도 뒤가 막히지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(
            flowOf(SynthesisProgress.Failed(0, "실패"), done(1), done(2), done(3)),
        )

        assertEquals(listOf(1, 2, 3), player.appended)
    }

    @Test
    fun `합성이 끝나면 알린다`() = runTest {
        val session = PlaybackSession(FakePlayer())
        session.load(script)

        assertFalse(session.state.value.synthesisComplete)
        session.consume(flowOf(done(0), SynthesisProgress.Complete))
        assertTrue(session.state.value.synthesisComplete)
    }

    @Test
    fun `전체 시각으로 시크하면 문장과 위치로 환산한다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        session.measured(0, 1_000)
        session.measured(1, 1_000)
        session.measured(2, 1_000)
        session.measured(3, 1_000)

        session.seekToOverall(2_500)

        assertEquals(2 to 500L, player.seeks.last())
    }

    /** 놓친 대목을 되감는 조작이라 문장 경계를 넘어가야 한다. */
    @Test
    fun `되감기가 앞 문장으로 넘어간다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        repeat(4) { session.measured(it, 5_000) }
        session.seekToSentence(2, withinMs = 1_000)

        session.rewind(byMs = 3_000)

        // 2번 문장의 1초 지점(전체 11초)에서 3초 되감으면 1번 문장의 3초 지점
        assertEquals(1 to 3_000L, player.seeks.last())
    }

    @Test
    fun `되감기가 처음을 넘어가지 않는다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        repeat(4) { session.measured(it, 5_000) }
        session.seekToSentence(0, withinMs = 500)

        session.rewind(byMs = 10_000)

        assertEquals(0 to 0L, player.seeks.last())
    }

    @Test
    fun `빨리 감기가 끝을 넘어가지 않는다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        repeat(4) { session.measured(it, 1_000) }
        session.seekToSentence(3, withinMs = 500)

        session.forward(byMs = 60_000)

        assertEquals(3 to 1_000L, player.seeks.last())
    }

    @Test
    fun `속도를 재생기에 그대로 넘긴다`() {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.setSpeed(1.5f)

        assertEquals(listOf(1.5f), player.speeds)
        assertEquals(1.5f, session.state.value.speed)
    }

    @Test
    fun `멈추면 이어듣기 지점을 적는다`() {
        val player = FakePlayer()
        val saved = mutableListOf<Pair<Int, Long>>()
        val session = PlaybackSession(player) { index, position -> saved += index to position }
        session.load(script)
        session.seekToSentence(2, withinMs = 700)

        session.pause()

        assertEquals(listOf(2 to 700L), saved)
    }

    @Test
    fun `이어듣기 지점에서 시작한다`() {
        val session = PlaybackSession(FakePlayer())

        session.load(script, startAt = 2)

        assertEquals(2, session.state.value.currentIndex)
    }

    @Test
    fun `이어듣기 지점이 문장 수를 넘으면 마지막으로 자른다`() {
        val session = PlaybackSession(FakePlayer())

        session.load(script, startAt = 99)

        assertEquals(3, session.state.value.currentIndex)
    }

    /** 화면을 끈 채 들을 때 지금 어디쯤인지 알 수 있는 유일한 창구다. */
    @Test
    fun `잠금화면에 띄울 제목과 문장을 넘긴다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        val withTitle = parseScript("# 이벤트 루프\n\n하나. 둘.")

        session.load(withTitle)
        session.consume(flowOf(done(0), done(1)))

        assertEquals("이벤트 루프", player.capturedTitle)
        assertEquals(listOf("이벤트 루프", "하나."), player.appendedTexts.take(2))
    }

    /**
     * 이어듣기의 요점.
     *
     * 20번 문장부터 들어야 하는데 0번이 붙는 순간 재생하면 이어듣기를 저장해 둔 의미가
     * 없다 — 처음부터 다시 듣게 된다. 그 문장이 붙을 때까지 기다려야 한다.
     */
    /**
     * 이어듣기 자리가 아직 안 붙었는데 재생을 누르면, 그 문장이 올 때까지 미룬다.
     * 지금 틀면 0번부터 나가는데 그것은 듣던 자리로 돌아가려던 것과 어긋난다.
     */
    @Test
    fun `이어듣기 자리가 준비되면 미뤄 둔 재생이 시작된다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2, startWithinMs = 700)

        session.consume(flowOf(done(0)))
        session.play()
        assertFalse("아직 그 문장이 안 붙었는데 재생했다", player.isPlaying)

        session.consume(flowOf(done(1), done(2)))
        assertTrue("그 문장이 붙었으니 시작해야 한다", player.isPlaying)
        // 목록이 2번 문장부터 시작하므로 그 문장은 항목 0번이다
        assertEquals(0 to 700L, player.seeks.last())
        assertEquals("화면에는 문장 번호로 보여야 한다", 2, session.state.value.currentIndex)
    }

    @Test
    fun `미뤄 둔 재생을 정지로 거둔다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2)
        session.consume(flowOf(done(0)))
        session.play()

        session.pause()
        session.consume(flowOf(done(1), done(2)))

        assertFalse("정지를 눌렀는데 잠시 뒤 켜졌다", player.isPlaying)
    }

    @Test
    fun `이어듣기 자리가 없으면 누른 즉시 재생한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 0, startWithinMs = 0)
        session.consume(flowOf(done(0)))

        session.play()

        assertTrue(player.isPlaying)
        assertTrue("옮길 자리가 없으니 시크도 없다", player.seeks.isEmpty())
    }

    @Test
    fun `이어듣기 자리가 실패한 문장이어도 미뤄 둔 재생이 시작된다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 1, startWithinMs = 500)
        session.play()

        session.consume(
            flowOf(done(0), SynthesisProgress.Failed(1, "실패"), done(2)),
        )

        assertTrue("실패한 자리에서 미뤄 둔 재생이 묶여 버렸다", player.isPlaying)
    }

    /**
     * 실기기에서 나온 회귀.
     *
     * 이어듣기를 고칠 때 자동재생 조건을 `itemCount == 1` 에서 `>= 1` 로 바꿨더니, 합성이
     * 끝난 문장이 올 때마다 "재생 중이 아니면 재생" 이 다시 걸렸다. 합성이 재생보다
     * 33배 빠르니 정지를 눌러도 곧바로 다음 문장이 도착해 다시 켜졌다 —
     * 정지가 아예 안 되는 것처럼 보였다.
     */
    @Test
    fun `한 번 재생을 시작한 뒤에는 스스로 다시 켜지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(flowOf(done(0)))
        session.play()
        assertTrue(player.isPlaying)

        session.pause()
        assertFalse(player.isPlaying)

        // 뒤 문장이 합성되어 들어와도 다시 켜지지 않아야 한다
        session.consume(flowOf(done(1), done(2), done(3)))
        assertFalse("합성이 들어오면서 재생이 되살아났다", player.isPlaying)
    }

    /**
     * 재생기의 실제 위치를 따라가야 한다.
     *
     * 문장을 자동으로 넘길 때 화면이 그것을 모르면 하이라이트가 첫 문장에 머문다.
     * 에어팟으로 정지했을 때 화면이 모르는 것도 같은 원인이다.
     */
    @Test
    fun `재생기의 실제 문장과 재생 여부를 따라간다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        session.consume(flowOf(done(0), done(1), done(2)))
        session.play()

        // 재생기가 스스로 2번 문장으로 넘어갔다
        player.currentIndex = 2
        player.positionMs = 400
        session.syncFromPlayer()

        assertEquals(2, session.state.value.currentIndex)
        assertTrue(session.state.value.playing)

        // 에어팟으로 정지했다 — 우리가 부른 것이 아니다
        player.isPlaying = false
        session.syncFromPlayer()

        assertFalse("바깥에서 멈춘 것을 따라가지 못했다", session.state.value.playing)
    }

    /** 화면을 떠나면 멈춰야 한다. 보이는 컨트롤이 없는 채로 소리가 나면 멈출 길이 없다. */
    @Test
    fun `멈추기를 부르면 재생을 세우고 진도를 적는다`() = runTest {
        val player = FakePlayer()
        val saved = mutableListOf<Pair<Int, Long>>()
        val session = PlaybackSession(player) { index, position -> saved += index to position }
        session.load(script)
        session.consume(flowOf(done(0)))
        session.play()
        player.currentIndex = 0
        player.positionMs = 1_200

        session.stop()

        assertFalse(player.isPlaying)
        assertEquals(listOf(0 to 1_200L), saved)
    }

    @Test
    fun `멈춘 뒤에 합성이 들어와도 다시 켜지지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)
        session.consume(flowOf(done(0)))
        session.play()
        session.stop()

        session.consume(flowOf(done(1)))

        assertFalse(player.isPlaying)
    }

    /**
     * 성능의 요점.
     *
     * 이어듣기로 열면 목록을 **듣던 문장부터** 만든다. 0번부터 채우면 그 자리에 닿기까지
     * 앞의 것을 다 합성해야 하는데, 실기기에서 그 대기가 길다는 불만이 나왔다.
     */
    @Test
    fun `이어듣기로 열면 목록이 듣던 문장부터 시작한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2)

        session.consume(flowOf(done(2), done(3)))

        assertEquals("듣던 문장이 먼저 붙어야 한다", listOf(2, 3), player.appended)
    }

    @Test
    fun `목록 시작보다 앞 문장은 아직 붙이지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2)

        session.consume(flowOf(done(0), done(1)))

        assertTrue("앞 문장이 목록에 붙었다", player.appended.isEmpty())
    }

    /** 앞으로 돌아갈 수 있어야 한다. 이미 만들어 둔 소리를 다시 붙이므로 합성은 없다. */
    @Test
    fun `앞 문장으로 가면 목록을 다시 만든다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2)
        session.consume(flowOf(done(2), done(3), done(0), done(1)))
        assertEquals(listOf(2, 3), player.appended)

        session.seekToSentence(0)

        assertEquals("0번부터 다시 붙어야 한다", listOf(0, 1, 2, 3), player.appended)
        assertEquals(0 to 0L, player.seeks.last())
        assertEquals(0, session.state.value.currentIndex)
    }

    @Test
    fun `재생기 항목 번호에 목록 시작을 더해 문장 번호를 낸다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2)
        session.consume(flowOf(done(2), done(3)))

        // 재생기는 항목 1번(= 문장 3번)을 재생 중이다
        player.currentIndex = 1
        player.isPlaying = true
        session.syncFromPlayer()

        assertEquals(3, session.state.value.currentIndex)
    }

    @Test
    fun `이어듣기 지점을 문장 번호로 적는다`() = runTest {
        val player = FakePlayer()
        val saved = mutableListOf<Pair<Int, Long>>()
        val session = PlaybackSession(player) { index, position -> saved += index to position }
        session.load(script, startAt = 2)
        session.consume(flowOf(done(2), done(3)))
        player.currentIndex = 1
        player.positionMs = 300

        session.saveProgress()

        assertEquals("항목 번호가 아니라 문장 번호여야 한다", listOf(3 to 300L), saved)
    }

    /**
     * 이어듣기 자리의 문장이 실패하면 미뤄 둔 재생이 영원히 묶였다.
     *
     * 붙이기 루프 안에서만 확인해서, 실패로 건너뛴 자리는 한 번도 검사되지 않았다 —
     * 재생 버튼이 죽은 것처럼 보인다.
     */
    @Test
    fun `이어듣기 자리가 실패해도 재생 버튼이 죽지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 1, startWithinMs = 500)
        session.play()

        session.consume(
            flowOf(SynthesisProgress.Failed(1, "실패"), done(2), done(3)),
        )

        assertTrue("미뤄 둔 재생이 풀리지 않았다", player.isPlaying)
    }

    @Test
    fun `합성이 다 끝난 뒤에 눌러도 재생된다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2, startWithinMs = 300)

        session.consume(flowOf(done(2), done(3), SynthesisProgress.Complete))
        session.play()

        assertTrue("합성이 끝난 뒤 재생이 안 됐다", player.isPlaying)
    }
}
