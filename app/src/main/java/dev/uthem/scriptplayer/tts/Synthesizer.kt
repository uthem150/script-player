package dev.uthem.scriptplayer.tts

import java.io.File

/**
 * 기기에 있는 음성 하나.
 *
 * [needsNetwork] 와 [notInstalled] 를 그대로 들고 있는다 — 걸러낸 결과만 넘기면
 * 화면이 "왜 이 음성은 목록에 없는가" 를 설명할 수 없다.
 */
data class VoiceInfo(
    val name: String,
    val language: String,
    val quality: Int,
    val needsNetwork: Boolean,
    val notInstalled: Boolean,
) {
    val usableOffline: Boolean get() = !needsNetwork && !notInstalled
}

/**
 * 문장 하나를 합성해 달라는 요청.
 *
 * **속도가 없다.** 합성은 항상 1.0 으로 한다 — 파일에 배속을 구우면 재생 시 배속과
 * 곱해져 통제할 수 없다. 속도는 재생기만의 관심사다.
 *
 * [pitch] 는 화자 구분용이라 문장의 속성이므로 여기 있다. 사용자의 전역 음높이 조절은
 * 재생 시점에 이 위로 곱해진다.
 */
data class SynthesisRequest(
    val text: String,
    val voiceName: String,
    val pitch: Float = 1.0f,
)

/** 글자 오프셋 → 시각. `onRangeStart` 가 알려주는 값에서 만든다. */
data class WordTiming(val charStart: Int, val charEnd: Int, val atMs: Long)

data class SynthesizedSentence(
    val audio: File,
    /** 엔진이 `onRangeStart` 를 보내지 않으면 빈 목록 */
    val wordTimings: List<WordTiming>,
)

interface Synthesizer {
    /** 이 기기에서 쓸 수 있는 음성 전부. 걸러내는 것은 부르는 쪽이 정한다. */
    suspend fun availableVoices(): List<VoiceInfo>

    /** 문장 하나를 합성한다. 캐시에 있으면 그것을 준다. */
    suspend fun synthesize(request: SynthesisRequest): Result<SynthesizedSentence>
}

/**
 * 오프라인으로 쓸 수 있는 한국어 음성만 고른다.
 *
 * **세 조건을 다 봐야 한다.** 실측한 기기에는 같은 화자가 `-local` 과 `-network` 로 짝지어
 * 있었다(`ko-kr-x-ism-local` · `ko-kr-x-ism-network`). 이름만 보고 고르면 네트워크 음성을
 * 집어 비행기 모드에서 조용히 실패한다.
 *
 * 순수 함수라 기기 없이 테스트한다 — 이 필터가 새면 오프라인이라는 약속이 깨진다.
 */
fun List<VoiceInfo>.offlineKorean(): List<VoiceInfo> =
    filter { it.language == "ko" && it.usableOffline }
        .sortedWith(compareByDescending<VoiceInfo> { it.quality }.thenBy { it.name })

/**
 * 화자에게 음성을 돌려 붙인다.
 *
 * 실측에서 오프라인 한국어 음성이 네 종 나왔으므로, 화자마다 **실제로 다른 목소리**를
 * 주는 것이 기본이다. 화자가 음성보다 많으면 음성을 돌려 쓰면서 음높이로 갈라 준다 —
 * 같은 목소리 같은 높이로 두 사람을 읽으면 대담이 혼잣말처럼 들린다.
 */
fun assignVoices(
    speakerIds: List<String>,
    voices: List<VoiceInfo>,
    /**
     * 사용자가 자리별로 정해 둔 음성 이름.
     *
     * 비어 있거나 이 기기에 없는 이름이면 자동 배정으로 넘어간다 — 폰을 바꾸면 음성
     * 이름이 달라질 수 있고, 그때 설정에 남은 옛 이름 때문에 소리가 안 나면 안 된다.
     */
    preferred: List<String> = emptyList(),
): Map<String, SynthesisRequest> {
    if (voices.isEmpty()) return emptyMap()
    val pitches = listOf(1.0f, 0.88f, 1.12f, 0.94f)
    return speakerIds.withIndex().associate { (index, id) ->
        val chosen = preferred.getOrNull(index)
            ?.takeIf { name -> voices.any { it.name == name } }

        val request = if (chosen != null) {
            SynthesisRequest(text = "", voiceName = chosen, pitch = 1.0f)
        } else {
            SynthesisRequest(
                text = "",
                voiceName = voices[index % voices.size].name,
                // 음성을 한 바퀴 다 쓴 뒤부터 음높이를 바꾼다
                pitch = pitches[(index / voices.size) % pitches.size],
            )
        }
        id to request
    }
}
