@file:OptIn(UnstableApi::class)

package dev.uthem.scriptplayer.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.uthem.scriptplayer.appContainer
import dev.uthem.scriptplayer.playback.Media3PlayerController
import dev.uthem.scriptplayer.playback.PlaybackService
import dev.uthem.scriptplayer.playback.PlaybackSession
import dev.uthem.scriptplayer.playback.positionForCharOffset
import dev.uthem.scriptplayer.tts.AndroidSynthesizer
import dev.uthem.scriptplayer.tts.SynthesisProgress
import dev.uthem.scriptplayer.tts.SynthesisQueue
import dev.uthem.scriptplayer.tts.SynthesisRequest
import dev.uthem.scriptplayer.tts.WordTiming
import dev.uthem.scriptplayer.tts.assignVoices
import dev.uthem.scriptplayer.tts.offlineKorean
import dev.uthem.scriptplayer.tts.wavDurationMs
import dev.uthem.scriptplayer.ui.voice.NoVoiceScreen
import dev.uthem.scriptplayer.ui.voice.rememberInstallVoiceAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 재생기에 실제 소리를 붙이는 층.
 *
 * 하는 일은 이어 붙이기다 — 보관함에서 대본을 읽고, 음성을 골라 화자에 배정하고, 합성 큐를
 * 돌려 그 결과를 재생 세션에 흘려보낸다. 각 조각은 이미 따로 테스트되어 있고, 여기서
 * 새로 판단하는 것은 없다.
 */
@Composable
fun PlayerRoute(
    scriptId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val installVoice = rememberInstallVoiceAction()

    var noVoice by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    var sentences by remember { mutableStateOf<List<PlayerSentence>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<PlaybackSession?>(null) }
    var uiState by remember { mutableStateOf(PlayerUiState()) }
    val timings = remember { mutableMapOf<Int, List<WordTiming>>() }
    val durations = remember { mutableMapOf<Int, Long>() }

    /*
     * [attempt] 가 바뀌면 처음부터 다시 한다 — 음성을 받고 돌아왔을 때 쓰인다.
     */
    LaunchedEffect(scriptId, attempt) {
        noVoice = false
        val script = container.scripts.load(scriptId) ?: run {
            onBack()
            return@LaunchedEffect
        }
        title = script.title
        sentences = script.sentences.map { PlayerSentence(it.text, it.speakerId?.let { id ->
            script.speakers.firstOrNull { speaker -> speaker.id == id }?.label
        }) }

        val synthesizer = AndroidSynthesizer(context, container.audioCache)
        val voices = if (synthesizer.open().isSuccess) {
            synthesizer.availableVoices().offlineKorean()
        } else {
            emptyList()
        }
        val controller = if (voices.isNotEmpty()) connectToService(context) else null
        if (voices.isEmpty() || controller == null) {
            synthesizer.close()
            noVoice = true
            return@LaunchedEffect
        }

        val progress = container.scripts.progressOf(scriptId)
        val active = PlaybackSession(Media3PlayerController(controller)) { index, positionMs ->
            /*
             * 이어듣기 지점을 적는다.
             *
             * 콜백은 동기라 여기서 데이터베이스를 기다리면 메인 스레드가 막힌다 —
             * 코루틴으로 넘긴다.
             */
            scope.launch { container.scripts.saveProgress(scriptId, index, positionMs) }
        }
        session = active
        active.load(
            script = script,
            startAt = progress.sentenceIndex,
            startWithinMs = progress.positionMs,
        )

        /*
         * 지난번 배속을 그대로 이어 쓴다.
         *
         * 늘 1.5배로 듣는 사람이 대본마다 다시 누르는 것이 실제 불편이다.
         */
        active.setSpeed(container.settings.speed.value)

        val speakerVoices = assignVoices(
            speakerIds = script.speakers.map { it.id },
            voices = voices,
            preferred = container.settings.speakerVoices.value,
        )
        val fallback = SynthesisRequest(text = "", voiceName = voices.first().name)

        SynthesisQueue(synthesizer)
            .synthesize(script, speakerVoices, fallback)
            .onEach { event ->
                if (event !is SynthesisProgress.Done) return@onEach
                /*
                 * 길이를 파일에서 바로 잰다.
                 *
                 * ExoPlayer 가 항목을 준비한 뒤 알려주는 값을 기다리면 진도 막대가 한동안
                 * 어림값으로 남는다. 파일은 이미 손에 있으니 지금 재는 것이 정확하고 빠르다.
                 */
                wavDurationMs(event.sentence.audio)?.let { durationMs ->
                    durations[event.index] = durationMs
                    active.measured(event.index, durationMs)
                }
                if (event.sentence.wordTimings.isNotEmpty()) {
                    timings[event.index] = event.sentence.wordTimings
                }
            }
            .let { active.consume(it) }

        synthesizer.close()
    }

    /*
     * 재생 위치를 주기적으로 읽는다.
     *
     * 재생기는 위치를 밀어 주지 않으므로 당겨 와야 한다. 200ms 마다 읽는다 — 시크바가
     * 부드럽게 움직이는 최소치이고, 더 자주 읽으면 배터리만 쓴다.
     */
    LaunchedEffect(session, sentences, title) {
        val active = session ?: return@LaunchedEffect
        while (true) {
            val playback = active.state.value
            uiState = PlayerUiState(
                title = title,
                sentences = sentences,
                currentIndex = playback.currentIndex,
                readySentences = playback.readySentences,
                failedSentences = playback.failedSentences,
                playing = playback.playing,
                positionMs = active.overallPositionMs(),
                totalMs = playback.totalMs,
                speed = playback.speed,
            )
            delay(200)
        }
    }

    // 화면을 떠날 때 들은 자리를 적는다
    DisposableEffect(session) {
        onDispose { session?.saveProgress() }
    }

    if (noVoice) {
        NoVoiceScreen(
            onOpenSettings = installVoice,
            onRetry = { attempt++ },
            modifier = modifier,
        )
        return
    }

    PlayerScreen(
        state = uiState,
        onBack = onBack,
        onTogglePlay = {
            val active = session ?: return@PlayerScreen
            if (uiState.playing) active.pause() else active.play()
        },
        onRewind = { session?.rewind() },
        onForward = { session?.forward() },
        onPreviousSentence = {
            session?.seekToSentence((uiState.currentIndex - 1).coerceAtLeast(0))
        },
        onNextSentence = {
            session?.seekToSentence(
                (uiState.currentIndex + 1).coerceAtMost(maxOf(0, uiState.sentenceCount - 1)),
            )
        },
        onSeek = { session?.seekToOverall(it) },
        onSpeed = { speed ->
            session?.setSpeed(speed)
            container.settings.setSpeed(speed)
        },
        onTapWord = { sentenceIndex, charOffset ->
            val active = session ?: return@PlayerScreen
            val text = uiState.sentences.getOrNull(sentenceIndex)?.text ?: return@PlayerScreen
            val within = positionForCharOffset(
                charOffset = charOffset,
                sentenceLength = text.length,
                durationMs = durations[sentenceIndex] ?: 0L,
                timings = timings[sentenceIndex].orEmpty(),
            )
            active.seekToSentence(sentenceIndex, within)
        },
        modifier = modifier,
    )
}

/** 서비스에 붙어 재생기를 얻는다. 붙지 못하면 null. */
private suspend fun connectToService(context: Context): MediaController? =
    suspendCancellableCoroutine { continuation ->
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val controller = runCatching { future.get() }.getOrNull()
                if (continuation.isActive) continuation.resumeWith(Result.success(controller))
            },
            ContextCompat.getMainExecutor(context),
        )
    }
