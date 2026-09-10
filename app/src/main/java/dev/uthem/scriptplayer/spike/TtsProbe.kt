package dev.uthem.scriptplayer.spike

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/*
 * 1단계 검증 앱 전용. 실측이 끝나면 spike 패키지째로 지운다.
 *
 * 여기서 재는 것은 네 가지다.
 *   1. 기기에 오프라인 한국어 음성이 있는가
 *   2. synthesizeToFile 이 실제로 얼마나 빠른가
 *   3. onRangeStart 가 단어 타이밍을 주는가
 *   4. (PlaybackProbeService) 미디어 버튼이 들어오는가
 */

data class VoiceRow(
    val name: String,
    val locale: String,
    val needsNetwork: Boolean,
    val notInstalled: Boolean,
    val quality: Int,
) {
    val usableOffline: Boolean get() = !needsNetwork && !notInstalled
}

data class SentenceProbe(
    val chars: Int,
    val elapsedMs: Long,
    val rangeCount: Int,
    val audioBytes: Long,
    val sampleRate: Int?,
    val error: String?,
)

class TtsProbe(private val context: Context) {

    private var engine: TextToSpeech? = null
    private val pending = mutableMapOf<String, (Result<Unit>) -> Unit>()
    private val ranges = mutableMapOf<String, MutableList<Int>>()
    private var counter = 0

    val defaultEngineName: String? get() = engine?.defaultEngine

    suspend fun open(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val created = TextToSpeech(context) { status ->
            if (!cont.isActive) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                cont.resume(Result.success(Unit))
            } else {
                cont.resume(Result.failure(IllegalStateException("TTS 초기화 실패 (status=$status)")))
            }
        }
        engine = created
        created.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { pending.remove(it)?.invoke(Result.success(Unit)) }
                }

                @Deprecated("errorCode 를 주는 쪽을 쓰지만, 추상 메서드라 구현은 남겨야 한다")
                override fun onError(utteranceId: String?) {
                    fail(utteranceId, "합성 실패")
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    fail(utteranceId, "합성 실패 (code=$errorCode)")
                }

                /**
                 * 이 콜백이 오는지가 단어 탭 재생의 정확도를 가른다.
                 * frame 은 생성된 오디오의 프레임 위치라, 샘플레이트로 나누면 시각이 된다.
                 */
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    utteranceId?.let { ranges.getOrPut(it) { mutableListOf() }.add(frame) }
                }

                private fun fail(utteranceId: String?, message: String) {
                    utteranceId?.let {
                        pending.remove(it)?.invoke(Result.failure(RuntimeException(message)))
                    }
                }
            },
        )
    }

    fun voices(): List<VoiceRow> {
        val all = engine?.voices ?: return emptyList()
        return all
            .map { voice ->
                VoiceRow(
                    name = voice.name,
                    locale = voice.locale.toString(),
                    needsNetwork = voice.isNetworkConnectionRequired,
                    notInstalled = voice.features
                        ?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
                    quality = voice.quality,
                )
            }
            .sortedWith(compareBy({ it.locale }, { it.name }))
    }

    fun koreanOfflineVoices(): List<Voice> {
        val all = engine?.voices ?: return emptyList()
        return all
            .filter { voice ->
                voice.locale.language == "ko" &&
                    !voice.isNetworkConnectionRequired &&
                    voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
            }
            .sortedByDescending { it.quality }
    }

    /**
     * 문장 하나를 파일로 합성하고 걸린 시간을 잰다.
     *
     * 속도는 **항상 1.0** 으로 합성한다. 파일에 배속을 구우면 재생 시 배속과 곱해져
     * 통제할 수 없다 — 속도는 재생기만의 관심사다.
     */
    suspend fun synthesize(text: String, voice: Voice?, outFile: File): SentenceProbe {
        val tts = engine ?: return SentenceProbe(text.length, 0, 0, 0, null, "엔진이 열리지 않음")
        voice?.let { tts.voice = it }
        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)

        val id = "probe-${counter++}"
        ranges[id] = mutableListOf()
        val startedAt = System.nanoTime()

        val outcome: Result<Unit> = suspendCancellableCoroutine { cont ->
            pending[id] = { result -> if (cont.isActive) cont.resume(result) }
            val queued = tts.synthesizeToFile(text, Bundle(), outFile, id)
            if (queued != TextToSpeech.SUCCESS) {
                pending.remove(id)
                if (cont.isActive) {
                    cont.resume(Result.failure(RuntimeException("큐에 넣지 못함 (ret=$queued)")))
                }
            }
        }

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        return SentenceProbe(
            chars = text.length,
            elapsedMs = elapsedMs,
            rangeCount = ranges.remove(id)?.size ?: 0,
            audioBytes = if (outFile.exists()) outFile.length() else 0,
            sampleRate = if (outFile.exists()) readWavSampleRate(outFile) else null,
            error = outcome.exceptionOrNull()?.message,
        )
    }

    fun close() {
        engine?.shutdown()
        engine = null
    }
}

/**
 * 검증용 거친 문장 나누기.
 *
 * 제대로 된 파서는 3단계에서 만든다. 여기서는 합성 속도를 재는 것이 목적이라
 * 문장 경계가 조금 틀려도 무방하다 — 대신 코드블록과 마크다운 기호는 걸러 낸다.
 * 그걸 그대로 읽히면 합성 시간이 실제보다 부풀어 측정이 흐려진다.
 */
fun crudeSentences(raw: String): List<String> =
    raw.replace(Regex("(?s)```.*?```"), " ")
        .replace(Regex("[#*`>|]"), "")
        .replace(Regex("^\\s*[-+]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*\\d+\\.\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .split(Regex("(?<=[.?!…])\\s+|\\n+"))
        .map { it.trim() }
        .filter { it.length > 1 }
