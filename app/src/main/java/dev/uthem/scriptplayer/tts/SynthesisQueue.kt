package dev.uthem.scriptplayer.tts

import dev.uthem.scriptplayer.parser.ParsedScript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 합성 진행 상태. 문장 하나가 끝날 때마다 하나씩 나온다. */
sealed interface SynthesisProgress {
    /**
     * 문장 하나가 준비됐다.
     *
     * 이것이 나오는 즉시 재생기가 플레이리스트에 붙인다. 그래서 첫 문장이 끝나면 바로
     * 소리가 나고, 나머지는 들으면서 채워진다 — 실측에서 합성이 재생보다 33배 빨랐다.
     */
    data class Done(val index: Int, val sentence: SynthesizedSentence) : SynthesisProgress

    /**
     * 문장 하나가 실패했다. 건너뛰고 계속한다.
     *
     * 한 문장 때문에 전체를 멈추지 않는다. 대본 하나에 특이한 글자가 섞여 있을 때
     * 그 문장만 조용히 비는 편이, 아무것도 못 듣는 것보다 낫다 — 화면에는 알린다.
     */
    data class Failed(val index: Int, val reason: String) : SynthesisProgress

    data object Complete : SynthesisProgress
}

/**
 * 대본 한 편을 문장 순서대로 합성한다.
 *
 * 화자별 음성 배정은 [assignVoices] 가 정하고, 여기서는 문장마다 그 화자의 설정을 붙여
 * 요청을 만든다. 화자가 없는 문장(머리글·서술)은 기본 음성으로 읽는다.
 *
 * `Flow` 로 내는 이유는 **순서를 지키면서 하나씩 흘려보내기 위해서다.** 전부 모아 한 번에
 * 주면 첫 소리까지 전체 합성을 기다려야 한다.
 */
class SynthesisQueue(private val synthesizer: Synthesizer) {

    fun synthesize(
        script: ParsedScript,
        voiceBySpeaker: Map<String, SynthesisRequest>,
        defaultVoice: SynthesisRequest,
        /**
         * 어느 문장부터 만들지.
         *
         * 이어듣기로 여는 경우 듣던 문장이 가장 급하다. 0번부터 만들면 그 자리에 닿기까지
         * 앞의 것을 다 기다려야 한다 — 80번째부터 듣던 대본이면 10초를 넘긴다.
         * 여기서부터 끝까지 먼저 만들고, 앞부분은 그 뒤에 채운다.
         */
        startAt: Int = 0,
    ): Flow<SynthesisProgress> = flow {
        orderedFrom(script.sentences.size, startAt).forEach { index ->
            val sentence = script.sentences[index]
            val settings = sentence.speakerId?.let { voiceBySpeaker[it] } ?: defaultVoice
            val request = SynthesisRequest(
                text = sentence.text,
                voiceName = settings.voiceName,
                pitch = settings.pitch,
            )

            synthesizer.synthesize(request).fold(
                onSuccess = { emit(SynthesisProgress.Done(sentence.index, it)) },
                onFailure = {
                    emit(
                        SynthesisProgress.Failed(
                            index = sentence.index,
                            reason = it.message ?: "알 수 없는 까닭",
                        ),
                    )
                },
            )
        }
        emit(SynthesisProgress.Complete)
    }
}

/**
 * [startAt] 부터 끝까지, 그다음 처음부터 [startAt] 앞까지.
 *
 * 앞부분을 버리지 않는 이유는 사용자가 뒤로 돌아갈 수 있어서다 — 급하지 않을 뿐이다.
 */
internal fun orderedFrom(count: Int, startAt: Int): List<Int> {
    if (count <= 0) return emptyList()
    val begin = startAt.coerceIn(0, count - 1)
    return (begin until count) + (0 until begin)
}
