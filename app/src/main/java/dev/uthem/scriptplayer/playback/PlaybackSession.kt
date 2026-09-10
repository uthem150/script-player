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
 * 2. 재생기의 실제 상태를 화면 상태로 옮긴다([syncFromPlayer])
 * 3. 전체 진도를 [SentenceTimeline] 으로 관리한다
 *
 * **스스로 재생을 시작하지 않는다.** 시작은 사용자가 정한다.
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

    /** 만들어 둔 소리 전부. 뒤로 돌아갈 때 다시 붙이는 데 쓴다. */
    private val ready = mutableMapOf<Int, java.io.File>()

    /**
     * 재생목록 0번이 어느 문장인지.
     *
     * 이어듣기로 열면 듣던 문장부터 목록을 만든다. 0번부터 채우면 그 자리에 닿기까지
     * 앞의 것을 다 합성해야 하는데, 80번째부터 듣던 대본이면 10초를 넘긴다.
     */
    private var playlistBase = 0

    /** 이어듣기로 옮겨야 할 자리. 그 문장이 붙을 때까지 기다린다. */
    private var pendingSeek: Pair<Int, Long>? = null

    /** 이어듣기 자리가 준비되기 전에 재생을 눌렀는지. 준비되면 그때 시작한다. */
    private var playWhenReady = false

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
        ready.clear()
        sentenceTexts = script.sentences.map { it.text }
        timeline = SentenceTimeline(script.sentences.map { it.text.length })
        val target = startAt.coerceIn(0, maxOf(0, script.sentences.size - 1))
        playlistBase = target
        nextToAppend = target
        // 목록이 듣던 문장부터 시작하므로, 남은 것은 그 문장 안에서의 위치뿐이다
        pendingSeek = if (startWithinMs > 0) target to startWithinMs else null
        playWhenReady = false
        _state.value = PlaybackUiState(
            sentenceCount = script.sentences.size,
            currentIndex = target,
            totalMs = timeline.totalMs,
        )
    }

    /**
     * 합성 진행을 받아 재생기에 붙인다.
     *
     * **스스로 재생을 시작하지 않는다.** 처음에는 첫 문장이 붙으면 바로 틀었는데, 대본을
     * 열어보려고 누른 사람에게 갑자기 소리가 터졌다. 합성은 열자마자 시작하므로 재생을
     * 누르면 즉시 나간다 — 기술적 이점은 그대로 두고 시작만 사용자가 정한다.
     */
    suspend fun consume(progress: Flow<SynthesisProgress>) {
        progress.collect { event ->
            when (event) {
                is SynthesisProgress.Done -> {
                    ready[event.index] = event.sentence.audio
                    waiting[event.index] = event.sentence.audio
                    drainInOrder()
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
            _state.value = _state.value.copy(readySentences = nextToAppend - playlistBase)
            applyPendingSeekIfReady()
        }
    }

    /** 이어듣기 자리의 문장이 붙었으면 그 안의 위치로 옮긴다. */
    private fun applyPendingSeekIfReady() {
        val (index, withinMs) = pendingSeek ?: return
        if (nextToAppend <= index) return
        pendingSeek = null
        player.seekTo(index - playlistBase, withinMs)
        _state.value = _state.value.copy(currentIndex = index)
        if (playWhenReady) {
            playWhenReady = false
            play()
        }
    }

    /**
     * 목록 시작보다 앞으로 갈 때 목록을 다시 만든다.
     *
     * 이어듣기로 열면 목록이 듣던 문장부터라, 그보다 앞은 목록에 없다. 이미 만들어 둔
     * 소리를 다시 붙이는 것이라 합성은 일어나지 않는다 — 콘텐츠 주소 캐시가 있는 값이다.
     */
    private fun rebuildFrom(sentenceIndex: Int) {
        player.clear()
        playlistBase = sentenceIndex
        nextToAppend = sentenceIndex
        while (true) {
            val audio = ready[nextToAppend] ?: break
            player.append(
                index = nextToAppend,
                audio = audio,
                sentenceText = sentenceTexts.getOrElse(nextToAppend) { "" },
            )
            nextToAppend++
        }
        _state.value = _state.value.copy(readySentences = nextToAppend - playlistBase)
    }

    /** 지금 재생 중인 **문장** 번호. */
    private fun currentSentence(): Int = playlistBase + player.currentIndex

    fun measured(index: Int, durationMs: Long) {
        timeline.measure(index, durationMs)
        _state.value = _state.value.copy(totalMs = timeline.totalMs)
    }

    /**
     * 재생.
     *
     * 이어듣기 자리가 아직 붙지 않았으면 그 문장이 올 때까지 미룬다. 지금 틀면 0번부터
     * 나가는데, 그것은 듣던 자리로 돌아가려던 것과 어긋난다. 합성이 재생보다 33배 빨라
     * 기다리는 시간은 길어도 몇 초이고, 그동안 화면에는 준비 표시가 떠 있다.
     */
    fun play() {
        if (pendingSeek != null) {
            playWhenReady = true
            return
        }
        player.play()
        _state.value = _state.value.copy(playing = true)
    }

    /**
     * 재생을 세우고 진도를 적는다.
     *
     * 화면을 떠날 때 부른다. 보이는 컨트롤이 없는 채로 소리가 계속 나면 멈출 길이 없다 —
     * 알림을 찾아야 한다.
     */
    fun stop() {
        playWhenReady = false
        player.pause()
        _state.value = _state.value.copy(playing = false)
        saveProgress()
    }

    /**
     * 재생기의 실제 상태를 상태에 옮긴다.
     *
     * 재생기는 밀어 주지 않으므로 당겨 와야 한다. 이것을 하지 않으면 문장이 자동으로
     * 넘어가도 하이라이트가 첫 문장에 머물고, 에어팟으로 정지한 것도 화면이 모른다.
     */
    fun syncFromPlayer() {
        val current = _state.value
        val playing = player.isPlaying
        /*
         * 이어듣기 자리로 옮기기 전에는 문장 번호를 재생기에서 가져오지 않는다.
         *
         * 그때 재생기에는 아직 아무것도 붙지 않아 0을 낸다. 그것으로 덮으면 저장해 둔
         * 자리가 화면에서 사라지고, 그 문장이 합성될 때까지 첫 문장만 보인다 —
         * "어디까지 들었는지 나오기까지 한참 걸린다" 가 이것이었다.
         */
        val index = if (pendingSeek != null) current.currentIndex else currentSentence()
        if (current.currentIndex == index && current.playing == playing) return
        _state.value = current.copy(currentIndex = index, playing = playing)
    }

    fun pause() {
        // 미뤄 둔 재생 요청도 함께 거둔다 — 정지를 눌렀는데 잠시 뒤 켜지면 안 된다
        playWhenReady = false
        player.pause()
        _state.value = _state.value.copy(playing = false)
        saveProgress()
    }

    /** 전체 시각으로 옮긴다. 시크바가 부른다. */
    fun seekToOverall(positionMs: Long) {
        val target = timeline.locate(positionMs)
        seekToSentence(target.sentenceIndex, target.withinMs)
    }

    fun seekToSentence(index: Int, withinMs: Long = 0) {
        // 목록 시작보다 앞이면 목록을 다시 만들어야 갈 수 있다
        if (index < playlistBase) rebuildFrom(index)
        player.seekTo(index - playlistBase, withinMs)
        _state.value = _state.value.copy(currentIndex = index)
    }

    /** 10초 되감기 — 놓친 대목을 다시 듣는 조작이라 문장 경계를 넘어간다. */
    fun rewind(byMs: Long = 10_000) {
        val here = timeline.positionOf(currentSentence(), player.positionMs)
        seekToOverall(here - byMs)
    }

    fun forward(byMs: Long = 10_000) {
        val here = timeline.positionOf(currentSentence(), player.positionMs)
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

    fun saveProgress() = onProgressSaved(currentSentence(), player.positionMs)

    /** 화면이 쓰는 전체 위치. */
    fun overallPositionMs(): Long = timeline.positionOf(currentSentence(), player.positionMs)
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
