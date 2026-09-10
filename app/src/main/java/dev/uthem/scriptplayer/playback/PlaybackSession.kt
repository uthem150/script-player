package dev.uthem.scriptplayer.playback

import dev.uthem.scriptplayer.parser.ParsedScript
import dev.uthem.scriptplayer.tts.SynthesisProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 합성과 재생을 잇는 자리.
 *
 * 하는 일은 셋이다.
 * 1. 합성이 끝난 문장을 **순서대로** 재생기에 붙인다
 * 2. 첫 문장이 붙는 순간 재생을 시작한다 (스트리밍 시작)
 * 3. 전체 진도를 [SentenceTimeline] 으로 관리한다
 *
 * 합성 결과가 순서대로 온다고 **가정하지 않는다.** 지금 구현은 순서대로 내지만, 나중에
 * 여러 문장을 나란히 합성하게 되면 뒤섞여 들어온다. 그때 소리가 뒤죽박죽 나는 것은
 * 찾기 어려운 버그라, 처음부터 순서를 지키게 해 둔다.
 */
class PlaybackSession(
    private val player: PlayerController,
    private val onProgressSaved: (sentenceIndex: Int, positionMs: Long) -> Unit = { _, _ -> },
) {

    private var timeline = SentenceTimeline(emptyList())
    private var sentenceTexts: List<String> = emptyList()

    /** 아직 붙이지 못한 문장 — 앞 문장이 오지 않아 기다리는 것들. */
    private val waiting = mutableMapOf<Int, java.io.File>()
    private var nextToAppend = 0

    /** 이어듣기로 옮겨야 할 자리. 그 문장이 붙을 때까지 기다린다. */
    private var pendingSeek: Pair<Int, Long>? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    /**
     * 새 대본을 걸고 준비한다.
     *
     * 이어듣기 지점은 바로 적용할 수 없다 — 아직 붙은 문장이 없어 그 자리로 갈 수 없다.
     * 기억해 두고 그 문장이 붙는 순간 옮긴다([pendingSeek]).
     */
    fun load(script: ParsedScript, startAt: Int = 0, startWithinMs: Long = 0) {
        player.clear()
        player.setScriptTitle(script.title)
        waiting.clear()
        nextToAppend = 0
        sentenceTexts = script.sentences.map { it.text }
        timeline = SentenceTimeline(script.sentences.map { it.text.length })
        val target = startAt.coerceIn(0, maxOf(0, script.sentences.size - 1))
        pendingSeek = if (target > 0 || startWithinMs > 0) target to startWithinMs else null
        _state.value = PlaybackUiState(
            sentenceCount = script.sentences.size,
            currentIndex = target,
            totalMs = timeline.totalMs,
        )
    }

    /**
     * 합성 진행을 받아 재생기에 붙인다.
     *
     * 첫 문장이 붙으면 바로 재생을 시작한다 — 실측에서 합성이 재생보다 33배 빨라,
     * 뒤는 들으면서 채워진다.
     */
    suspend fun consume(progress: Flow<SynthesisProgress>, autoPlay: Boolean = true) {
        progress.collect { event ->
            when (event) {
                is SynthesisProgress.Done -> {
                    waiting[event.index] = event.sentence.audio
                    drainInOrder()
                    /*
                     * 이어듣기 자리가 남아 있으면 아직 재생하지 않는다.
                     *
                     * 20번 문장부터 들어야 하는데 0번이 붙는 순간 재생하면, 이어듣기를
                     * 저장해 둔 의미가 없다 — 처음부터 다시 듣게 된다.
                     */
                    if (autoPlay && pendingSeek == null && player.itemCount >= 1 && !player.isPlaying) {
                        player.play()
                        _state.value = _state.value.copy(playing = true)
                    }
                }

                is SynthesisProgress.Failed -> {
                    /*
                     * 실패한 문장은 자리를 비우지 않고 **건너뛴다.**
                     *
                     * 자리를 비워 두면 뒤 문장이 영원히 기다린다 — 순서대로만 붙이기
                     * 때문이다. 그래서 다음 번호로 넘기고, 화면에는 알린다.
                     */
                    if (event.index == nextToAppend) nextToAppend++
                    drainInOrder()
                    _state.value = _state.value.copy(
                        failedSentences = _state.value.failedSentences + event.index,
                    )
                }

                SynthesisProgress.Complete ->
                    _state.value = _state.value.copy(synthesisComplete = true)
            }
        }
    }

    /** 준비된 것부터 순서대로 붙인다. 가운데가 비면 거기서 멈춘다. */
    private fun drainInOrder() {
        while (true) {
            val audio = waiting.remove(nextToAppend) ?: return
            player.append(
                index = nextToAppend,
                audio = audio,
                sentenceText = sentenceTexts.getOrElse(nextToAppend) { "" },
            )
            nextToAppend++
            _state.value = _state.value.copy(readySentences = nextToAppend)
            applyPendingSeekIfReady()
        }
    }

    /** 이어듣기 자리의 문장이 붙었으면 그리로 옮긴다. */
    private fun applyPendingSeekIfReady() {
        val (index, withinMs) = pendingSeek ?: return
        if (nextToAppend <= index) return
        pendingSeek = null
        player.seekTo(index, withinMs)
        _state.value = _state.value.copy(currentIndex = index)
    }

    fun measured(index: Int, durationMs: Long) {
        timeline.measure(index, durationMs)
        _state.value = _state.value.copy(totalMs = timeline.totalMs)
    }

    fun play() {
        player.play()
        _state.value = _state.value.copy(playing = true)
    }

    fun pause() {
        player.pause()
        _state.value = _state.value.copy(playing = false)
        saveProgress()
    }

    /** 전체 시각으로 옮긴다. 시크바가 부른다. */
    fun seekToOverall(positionMs: Long) {
        val target = timeline.locate(positionMs)
        player.seekTo(target.sentenceIndex, target.withinMs)
        _state.value = _state.value.copy(currentIndex = target.sentenceIndex)
    }

    fun seekToSentence(index: Int, withinMs: Long = 0) {
        player.seekTo(index, withinMs)
        _state.value = _state.value.copy(currentIndex = index)
    }

    /** 10초 되감기 — 놓친 대목을 다시 듣는 조작이라 문장 경계를 넘어간다. */
    fun rewind(byMs: Long = 10_000) {
        val here = timeline.positionOf(player.currentIndex, player.positionMs)
        seekToOverall(here - byMs)
    }

    fun forward(byMs: Long = 10_000) {
        val here = timeline.positionOf(player.currentIndex, player.positionMs)
        seekToOverall(here + byMs)
    }

    fun setSpeed(speed: Float) {
        player.setSpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setPitch(pitch: Float) {
        player.setPitch(pitch)
        _state.value = _state.value.copy(pitch = pitch)
    }

    fun saveProgress() = onProgressSaved(player.currentIndex, player.positionMs)

    /** 화면이 쓰는 전체 위치. */
    fun overallPositionMs(): Long = timeline.positionOf(player.currentIndex, player.positionMs)
}

data class PlaybackUiState(
    val sentenceCount: Int = 0,
    val currentIndex: Int = 0,
    /** 합성이 끝나 재생기에 붙은 문장 수 */
    val readySentences: Int = 0,
    val failedSentences: Set<Int> = emptySet(),
    val synthesisComplete: Boolean = false,
    val playing: Boolean = false,
    val totalMs: Long = 0,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
)
