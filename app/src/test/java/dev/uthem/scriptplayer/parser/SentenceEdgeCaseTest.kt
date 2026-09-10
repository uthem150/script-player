package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 문장 경계의 예외들.
 *
 * 여기서 잘못 끊기면 소리가 어색해진다. "삼 점" 하고 멈췄다가 "일사 입니다" 로 이어지면
 * 무슨 말인지 알 수 없다.
 */
class SentenceEdgeCaseTest {

    private fun textsOf(raw: String) = parseScript(raw).sentences.map { it.text }

    @Test
    fun `소수점에서 끊지 않는다`() {
        assertEquals(listOf("원주율은 3.14 입니다."), textsOf("원주율은 3.14 입니다."))
        assertEquals(listOf("버전 1.11.0 을 씁니다."), textsOf("버전 1.11.0 을 씁니다."))
    }

    @Test
    fun `영문 약어에서 끊지 않는다`() {
        assertEquals(
            listOf("예를 들어 e.g. 이런 경우입니다."),
            textsOf("예를 들어 e.g. 이런 경우입니다."),
        )
        assertEquals(
            listOf("A vs. B 를 견줍니다."),
            textsOf("A vs. B 를 견줍니다."),
        )
        assertEquals(
            listOf("점검 목록 etc. 을 확인합니다."),
            textsOf("점검 목록 etc. 을 확인합니다."),
        )
    }

    @Test
    fun `말줄임표는 한 번만 끊는다`() {
        assertEquals(listOf("그런데... 그게 아니었습니다."), textsOf("그런데... 그게 아니었습니다."))
        assertEquals(listOf("그런데… 그게 아니었습니다."), textsOf("그런데… 그게 아니었습니다."))
    }

    @Test
    fun `느낌표와 물음표가 겹쳐도 한 문장이다`() {
        assertEquals(listOf("정말인가요?!", "네."), textsOf("정말인가요?! 네."))
    }

    @Test
    fun `아주 긴 문장은 쉼표에서 나눈다`() {
        // TTS 엔진이 긴 입력에서 불안정해진다. 200자를 넘으면 쉼표에서 보조 분할한다
        val long = buildString {
            repeat(8) { append("이것은 아주 긴 설명이고 쉼표로 이어지는 문장입니다, ") }
            append("끝입니다.")
        }
        val texts = textsOf(long)

        assertTrue("나뉘지 않았다 (${texts.size}개)", texts.size > 1)
        texts.forEach { assertTrue("아직 200자를 넘는다: ${it.length}자", it.length <= 200) }
    }

    @Test
    fun `쉼표로 나눈 조각들을 이으면 원래 내용이 남는다`() {
        val long = buildString {
            repeat(8) { append("이것은 아주 긴 설명이고 쉼표로 이어지는 문장입니다, ") }
            append("끝입니다.")
        }
        val joined = textsOf(long).joinToString(" ")

        // 글자를 잃지 않았는지 본다 — 나누기가 내용을 먹으면 소리에서 빠진다
        assertEquals(long.replace(Regex("\\s+"), " ").trim(), joined)
    }

    @Test
    fun `쉼표가 없는 아주 긴 문장도 200자 이하로 나눈다`() {
        val long = "가".repeat(500) + "."
        val texts = textsOf(long)

        assertTrue("나뉘지 않았다", texts.size > 1)
        texts.forEach { assertTrue("아직 200자를 넘는다: ${it.length}자", it.length <= 200) }
    }

    @Test
    fun `200자 이하 문장은 건드리지 않는다`() {
        val raw = "짧은 문장입니다, 쉼표가 있지만 나누지 않습니다."

        assertEquals(listOf(raw), textsOf(raw))
    }

    @Test
    fun `나눈 조각의 원문 범위도 순서대로 이어진다`() {
        val long = buildString {
            repeat(8) { append("이것은 아주 긴 설명이고 쉼표로 이어지는 문장입니다, ") }
            append("끝입니다.")
        }
        val parsed = parseScript(long)

        assertTrue(parsed.sentences.size > 1)
        var previousEnd = -1
        parsed.sentences.forEach { sentence ->
            assertTrue(
                "문장 ${sentence.index} 의 시작이 앞 문장 끝보다 앞이다",
                sentence.sourceRange.first > previousEnd,
            )
            previousEnd = sentence.sourceRange.last
        }
    }
}
