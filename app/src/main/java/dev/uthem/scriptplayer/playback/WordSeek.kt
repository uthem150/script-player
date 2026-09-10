package dev.uthem.scriptplayer.playback

import dev.uthem.scriptplayer.tts.WordTiming

/**
 * 탭한 글자에서 재생 위치를 낸다.
 *
 * 단어 타이밍이 있으면 **실제 값**을 쓴다 — 합성할 때 `onRangeStart` 가 알려준 것이라
 * 정확하다. 없으면 글자 비율로 어림한다.
 *
 * 순수 함수로 둔 이유는 이것이 "단어를 탭해 그 지점부터 듣기" 의 전부이고, 화면으로는
 * 몇 밀리초 어긋났는지 확인할 수 없기 때문이다.
 */
fun positionForCharOffset(
    charOffset: Int,
    sentenceLength: Int,
    durationMs: Long,
    timings: List<WordTiming>,
): Long {
    if (durationMs <= 0) return 0
    val offset = charOffset.coerceIn(0, maxOf(0, sentenceLength))

    if (timings.isNotEmpty()) {
        /*
         * 탭한 자리를 **넘지 않는** 마지막 단어의 시작으로 간다.
         *
         * 단어 중간을 눌렀을 때 그 단어의 처음부터 읽는 것이 자연스럽다. 다음 단어로
         * 넘기면 방금 누른 낱말을 건너뛰고 읽어, 누른 것과 들리는 것이 어긋난다.
         */
        val hit = timings.lastOrNull { it.charStart <= offset } ?: timings.first()
        return hit.atMs.coerceIn(0, durationMs)
    }

    if (sentenceLength <= 0) return 0
    return (durationMs * offset / sentenceLength).coerceIn(0, durationMs)
}

/**
 * 지금 읽고 있는 단어의 글자 범위.
 *
 * 타이밍이 없으면 null 이다 — 그때는 단어 하이라이트를 끄고 문장 하이라이트만 남긴다.
 * 어림값으로 단어를 짚으면 실제로 읽는 것과 어긋나 보여, 없는 것보다 어수선하다.
 */
fun currentWordRange(positionMs: Long, timings: List<WordTiming>): IntRange? {
    if (timings.isEmpty()) return null
    val hit = timings.lastOrNull { it.atMs <= positionMs } ?: return null
    return hit.charStart until hit.charEnd
}
