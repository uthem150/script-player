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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    // 놓아주려면 들고 있어야 한다. 안 놓으면 세션 연결이 새고 알림이 남는다
    var controller by remember { mutableStateOf<MediaController?>(null) }
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

        /*
         * TTS 엔진 열기와 서비스 연결을 나란히 한다.
         *
         * 둘은 서로를 기다릴 이유가 없는데 순서대로 하면 각자의 시간이 그대로 더해진다 —
         * 엔진 초기화가 1~2초, 세션 연결이 0.5초쯤이라 그 차이가 눈에 띈다.
         */
        val synthesizer = AndroidSynthesizer(context, container.audioCache)
        val (voices, connected) = coroutineScope {
            val opening = async {
                if (synthesizer.open().isSuccess) {
                    synthesizer.availableVoices().offlineKorean()
                } else {
                    emptyList()
                }
            }
            val connecting = async { connectToService(context) }
            opening.await() to connecting.await()
        }
        controller = connected
        if (voices.isEmpty() || connected == null) {
            synthesizer.close()
            noVoice = true
            return@LaunchedEffect
        }

        val progress = container.scripts.progressOf(scriptId)
        val active = PlaybackSession(Media3PlayerController(connected)) { index, positionMs ->
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
            .synthesize(
                script = script,
                voiceBySpeaker = speakerVoices,
                defaultVoice = fallback,
                // 듣던 문장을 가장 먼저 만든다 — 그 자리에 닿기까지 앞의 것을 기다리지 않게
                startAt = progress.sentenceIndex,
            )
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
            /*
             * 재생기의 실제 문장과 재생 여부를 먼저 당겨 온다.
             *
             * 이것을 빼면 문장이 자동으로 넘어가도 하이라이트가 첫 문장에 머물고,
             * 에어팟으로 정지한 것도 화면이 모른다 — 실기기에서 둘 다 나왔다.
             */
            active.syncFromPlayer()
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

    /*
     * 화면을 떠나면 멈추고 놓아준다.
     *
     * 진도만 적고 두었더니 보관함으로 나가도 소리가 계속 났다. 보이는 컨트롤이 없는 채로
     * 재생되면 멈출 길이 없다 — 알림을 찾아야 한다. 화면을 끈 뒤의 이어 듣기는 이 화면에
     * 머무는 동안의 일이고, 나가는 것은 "그만 듣겠다" 는 뜻이다.
     */
    /*
     * 키를 [scriptId] 로만 둔다.
     *
     * 처음에 (session, controller) 를 키로 뒀는데, 둘이 서로 다른 시점에 설정되어
     * session 이 채워질 때 이전 이펙트가 정리되면서 **방금 연결한 컨트롤러를 놓아버렸다.**
     * 그 뒤로는 play() 가 아무 일도 하지 않는다 — "변환이 끝났는데 재생이 안 된다" 가
     * 이것이었다. onDispose 안에서 지금 값을 읽으므로 키로 잡을 이유도 없다.
     */
    DisposableEffect(scriptId) {
        onDispose {
            session?.stop()
            controller?.release()
        }
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
