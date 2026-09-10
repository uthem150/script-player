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

        override fun seekTo(sentenceIndex: Int, withinMs: Long) {
            seeks += sentenceIndex to withinMs
            currentIndex = sentenceIndex
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

    @Test
    fun `첫 문장이 붙으면 바로 재생을 시작한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script)

        session.consume(flowOf(done(0)))

        assertTrue("첫 문장에서 재생이 시작돼야 한다", player.isPlaying)
        assertEquals(listOf(0), player.appended)
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
    @Test
    fun `이어듣기 자리가 남아 있으면 그때까지 재생하지 않는다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 2, startWithinMs = 700)

        session.consume(flowOf(done(0)))
        assertFalse("아직 그 문장이 안 붙었는데 재생했다", player.isPlaying)

        session.consume(flowOf(done(1), done(2)))
        assertTrue("그 문장이 붙었으니 재생해야 한다", player.isPlaying)
        assertEquals(2 to 700L, player.seeks.last())
    }

    @Test
    fun `이어듣기 자리가 없으면 첫 문장에서 바로 재생한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 0, startWithinMs = 0)

        session.consume(flowOf(done(0)))

        assertTrue(player.isPlaying)
        assertTrue("옮길 자리가 없으니 시크도 없다", player.seeks.isEmpty())
    }

    @Test
    fun `이어듣기 자리가 실패한 문장이면 뒤 문장에서 재생을 시작한다`() = runTest {
        val player = FakePlayer()
        val session = PlaybackSession(player)
        session.load(script, startAt = 1, startWithinMs = 500)

        session.consume(
            flowOf(done(0), SynthesisProgress.Failed(1, "실패"), done(2)),
        )

        // 1번이 없으니 그 자리로 갈 수 없다 — 그래도 재생은 시작돼야 한다
        assertTrue("실패한 자리에서 멈춰 버렸다", player.isPlaying)
    }
}
