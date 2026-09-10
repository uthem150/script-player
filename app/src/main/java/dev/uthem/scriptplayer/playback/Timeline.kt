package dev.uthem.scriptplayer.playback

/**
 * 문장별 길이를 모아 전체 진도를 계산한다.
 *
 * 문장 하나가 파일 하나이므로 재생기는 "지금 항목의 어디" 만 알려 준다. 전체 대본에서
 * 어디쯤인지는 앞 문장들의 길이를 더해야 나오고, 그 누적합을 여기서 관리한다.
 *
 * 어려운 점은 **아직 합성되지 않은 문장의 길이를 모른다**는 것이다. 글자 수로 어림해
 * 채우고, 실제 길이가 들어오면 바꿔 넣는다.
 *
 * 순수 자료구조라 안드로이드 없이 테스트한다 — 진도 막대가 뒤로 튀는지는 눈으로
 * 확인하기 어렵고, 튀면 사용자는 재생기가 고장난 줄 안다.
 */
class SentenceTimeline(private val charCounts: List<Int>) {

    private val durations = LongArray(charCounts.size) { estimate(charCounts[it]) }
    private val measured = BooleanArray(charCounts.size)

    val size: Int get() = charCounts.size

    /**
     * 실제 길이를 받아 넣는다.
     *
     * 어림값보다 짧아도 그대로 받는다 — 실측이 어림보다 낫다. 다만 [totalMs] 가 줄어들
     * 수 있으므로, 진도 비율은 늘 지금의 [totalMs] 로 다시 계산해야 한다.
     */
    fun measure(index: Int, durationMs: Long) {
        if (index !in durations.indices || durationMs <= 0) return
        durations[index] = durationMs
        measured[index] = true
    }

    fun durationOf(index: Int): Long = durations.getOrElse(index) { 0 }

    fun isMeasured(index: Int): Boolean = measured.getOrElse(index) { false }

    val totalMs: Long get() = durations.sum()

    /** [index] 번째 문장이 시작하는 시각. */
    fun startOf(index: Int): Long {
        if (index <= 0) return 0
        var sum = 0L
        for (at in 0 until minOf(index, durations.size)) sum += durations[at]
        return sum
    }

    /** 전체에서의 현재 위치. */
    fun positionOf(index: Int, withinMs: Long): Long =
        startOf(index) + withinMs.coerceIn(0, durationOf(index))

    /**
     * 전체 시각을 문장과 그 안의 위치로 되돌린다.
     *
     * 시크바를 끌었을 때 어느 문장의 어디로 가야 하는지 정한다.
     */
    fun locate(positionMs: Long): Location {
        if (durations.isEmpty()) return Location(0, 0)
        var remaining = positionMs.coerceAtLeast(0)
        durations.forEachIndexed { index, duration ->
            if (remaining < duration) return Location(index, remaining)
            remaining -= duration
        }
        // 끝을 넘어가면 마지막 문장의 끝으로
        val last = durations.lastIndex
        return Location(last, durations[last])
    }

    data class Location(val sentenceIndex: Int, val withinMs: Long)

    companion object {
        /**
         * 글자 수로 길이를 어림한다.
         *
         * 실측(스펙 §12)에서 41문장 1300자가 오디오 180초였다 — 글자당 약 138ms.
         * 어림값은 실제보다 **작게** 잡는다. 크게 잡으면 실측이 들어올 때마다 전체 길이가
         * 줄어, 진도 막대가 앞으로 튀어 보인다. 짧게 잡으면 늘어나는 쪽이라 덜 거슬린다.
         */
        internal fun estimate(chars: Int): Long = (chars * 120L).coerceAtLeast(300L)
    }
}
