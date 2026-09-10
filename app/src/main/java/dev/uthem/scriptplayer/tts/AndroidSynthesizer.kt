package dev.uthem.scriptplayer.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dev.uthem.scriptplayer.data.AudioCache
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

/**
 * `TextToSpeech` 를 인터페이스 뒤로 감춘다.
 *
 * 플랫폼 함정이 이 파일 안에만 머물게 한다 — 비동기 초기화, 콜백으로 오는 완료,
 * 엔진마다 다른 `onRangeStart` 지원 여부. 조율하는 쪽은 이것들을 몰라도 된다.
 */
class AndroidSynthesizer(
    private val context: Context,
    private val cache: AudioCache,
) : Synthesizer {

    private var engine: TextToSpeech? = null
    private val pending = ConcurrentHashMap<String, (Result<Unit>) -> Unit>()
    private val ranges = ConcurrentHashMap<String, MutableList<Pair<IntRange, Int>>>()
    private val utteranceCounter = AtomicLong()

    /**
     * 엔진 이름과 버전. 캐시 키에 들어간다.
     *
     * 버전을 못 읽으면 이름만 쓴다 — 키가 흔들리는 것보다는 갱신을 놓치는 쪽이 낫다.
     * 흔들리면 매번 전부 다시 합성한다.
     */
    private val engineId: String
        get() {
            val name = engine?.defaultEngine ?: "unknown"
            val version = runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(name, 0).versionName
            }.getOrNull() ?: "?"
            return "$name@$version"
        }

    suspend fun open(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val created = TextToSpeech(context) { status ->
            if (!continuation.isActive) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(
                    Result.failure(IllegalStateException("TTS 초기화 실패 (status=$status)")),
                )
            }
        }
        engine = created
        created.setOnUtteranceProgressListener(Listener())
    }

    override suspend fun availableVoices(): List<VoiceInfo> =
        engine?.voices.orEmpty().map { it.toInfo() }.sortedBy { it.name }

    /**
     * 문장 하나를 합성한다.
     *
     * 캐시에 있으면 그것을 준다 — 두 번째 재생부터는 합성이 아예 일어나지 않는다.
     * 없으면 합성하고, 실패하면 **반쯤 쓰인 파일을 지운다.** 남겨 두면 다음에 캐시에 있는
     * 것으로 보여 소리 없는 문장이 된다.
     */
    override suspend fun synthesize(request: SynthesisRequest): Result<SynthesizedSentence> {
        val tts = engine ?: return Result.failure(IllegalStateException("엔진이 열리지 않음"))
        val key = cache.keyOf(request, engineId)
        cache.find(key)?.let { return Result.success(it) }

        val voice = tts.voices?.firstOrNull { it.name == request.voiceName }
            ?: return Result.failure(IllegalStateException("음성을 찾지 못함: ${request.voiceName}"))

        tts.voice = voice
        // 속도는 늘 1.0 이다. 파일에 배속을 구우면 재생 시 배속과 곱해져 통제할 수 없다
        tts.setSpeechRate(1.0f)
        tts.setPitch(request.pitch)

        val id = "s${utteranceCounter.incrementAndGet()}"
        ranges[id] = mutableListOf()
        val target = cache.audioFile(key)

        val outcome: Result<Unit> = suspendCancellableCoroutine { continuation ->
            pending[id] = { result -> if (continuation.isActive) continuation.resume(result) }
            val queued = tts.synthesizeToFile(request.text, Bundle(), target, id)
            if (queued != TextToSpeech.SUCCESS) {
                pending.remove(id)
                if (continuation.isActive) {
                    continuation.resume(
                        Result.failure(IllegalStateException("큐에 넣지 못함 (ret=$queued)")),
                    )
                }
            }
        }

        val collected = ranges.remove(id).orEmpty()
        return outcome.fold(
            onSuccess = {
                val timings = collected.toTimings(target)
                cache.storeTimings(key, timings)
                Result.success(SynthesizedSentence(audio = target, wordTimings = timings))
            },
            onFailure = { cause ->
                // 반쯤 쓰인 파일을 남기면 다음에 캐시에 있는 것으로 보여 소리가 빠진다
                target.delete()
                cache.timingFile(key).delete()
                Result.failure(cause)
            },
        )
    }

    fun close() {
        engine?.shutdown()
        engine = null
        pending.clear()
        ranges.clear()
    }

    private inner class Listener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utteranceId?.let { pending.remove(it)?.invoke(Result.success(Unit)) }
        }

        @Deprecated("errorCode 를 주는 쪽을 쓰지만, 추상 메서드라 구현은 남겨야 한다")
        override fun onError(utteranceId: String?) = fail(utteranceId, "합성 실패")

        override fun onError(utteranceId: String?, errorCode: Int) =
            fail(utteranceId, "합성 실패 (code=$errorCode)")

        /**
         * 단어 타이밍의 원천.
         *
         * `frame` 은 만들어지는 오디오의 프레임 위치다. 샘플레이트로 나눠야 시각이 되는데,
         * 이 시점에는 파일이 아직 완성되지 않아 헤더를 읽을 수 없다 — 그래서 프레임만
         * 모아 두고 합성이 끝난 뒤에 환산한다.
         */
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            utteranceId?.let { ranges[it]?.add((start until end) to frame) }
        }

        private fun fail(utteranceId: String?, message: String) {
            utteranceId?.let {
                pending.remove(it)?.invoke(Result.failure(RuntimeException(message)))
            }
        }
    }
}

private fun Voice.toInfo() = VoiceInfo(
    name = name,
    language = locale.language,
    quality = quality,
    needsNetwork = isNetworkConnectionRequired,
    notInstalled = features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
)

/**
 * 프레임 위치를 시각으로 바꾼다.
 *
 * 샘플레이트는 완성된 WAV 헤더에서 읽는다 — 엔진이 말해 주는 값을 믿지 않는다.
 * 음성마다 다를 수 있고, 실측에서 Google TTS 한국어는 24000Hz 였다.
 * 읽지 못하면 타이밍을 버린다. 틀린 시각으로 단어를 짚는 것보다 없는 것이 낫다.
 */
internal fun List<Pair<IntRange, Int>>.toTimings(audio: File): List<WordTiming> {
    if (isEmpty()) return emptyList()
    val sampleRate = readWavSampleRate(audio) ?: return emptyList()
    return map { (chars, frame) ->
        WordTiming(
            charStart = chars.first,
            charEnd = chars.last + 1,
            atMs = frame * 1000L / sampleRate,
        )
    }
}
