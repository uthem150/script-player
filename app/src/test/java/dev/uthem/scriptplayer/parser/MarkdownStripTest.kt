package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 마크다운 기호가 소리로 새어 나오지 않게 한다.
 *
 * 정리하지 않으면 "샵샵 별표 진행자 콜론" 같은 소리가 난다. AI 가 뽑아 주는 대본은
 * 거의 항상 마크다운이라 이 정리가 이 앱의 실질적인 값이다.
 */
class MarkdownStripTest {

    private fun textsOf(raw: String) = parseScript(raw).sentences.map { it.text }

    @Test
    fun `머리글 마커는 벗기고 본문은 남긴다`() {
        assertEquals(listOf("이벤트 루프 이해하기"), textsOf("# 이벤트 루프 이해하기"))
        assertEquals(listOf("작은 머리글"), textsOf("###### 작은 머리글"))
    }

    @Test
    fun `굵게와 기울임 마커를 벗긴다`() {
        assertEquals(listOf("정말 중요한 이야기입니다."), textsOf("**정말 중요한** 이야기입니다."))
        assertEquals(listOf("기울인 말도 그대로 읽힙니다."), textsOf("*기울인 말도* 그대로 읽힙니다."))
        assertEquals(listOf("밑줄로 강조한 것도 마찬가지."), textsOf("_밑줄로 강조한 것도_ 마찬가지."))
    }

    @Test
    fun `인라인 코드 백틱을 벗긴다`() {
        assertEquals(listOf("setTimeout 을 0으로 줘도 그렇습니다."), textsOf("`setTimeout` 을 0으로 줘도 그렇습니다."))
    }

    @Test
    fun `링크는 라벨만 남긴다`() {
        assertEquals(
            listOf("자세한 것은 문서에 있습니다."),
            textsOf("자세한 것은 [문서](https://example.com/very/long/path)에 있습니다."),
        )
    }

    @Test
    fun `이미지는 통째로 지운다`() {
        // 주소를 읽어 봐야 소음이고, 대체 텍스트만 읽으면 문장이 어그러진다
        assertEquals(listOf("그림 다음 문장입니다."), textsOf("![도표](a.png)\n그림 다음 문장입니다."))
    }

    @Test
    fun `구분선은 문장이 되지 않는다`() {
        assertEquals(listOf("앞", "뒤"), textsOf("앞\n---\n뒤"))
        assertEquals(listOf("앞", "뒤"), textsOf("앞\n***\n뒤"))
    }

    @Test
    fun `리스트 마커를 벗기고 항목은 각각 문장이 된다`() {
        assertEquals(
            listOf("첫째 항목", "둘째 항목", "셋째 항목"),
            textsOf("- 첫째 항목\n* 둘째 항목\n+ 셋째 항목"),
        )
        assertEquals(
            listOf("하나", "둘"),
            textsOf("1. 하나\n2. 둘"),
        )
    }

    @Test
    fun `인용 마커를 벗긴다`() {
        assertEquals(listOf("인용된 말입니다."), textsOf("> 인용된 말입니다."))
    }

    /**
     * 원문 범위의 계약.
     *
     * 범위는 **읽을 내용이 시작하는 자리부터 끝나는 자리까지**다. 그래서 앞뒤의 마커는
     * 범위 밖이고, 사이에 낀 마커는 범위 안에 남는다. 마커를 포함하도록 넓히지 않는다 —
     * 그러면 어디까지가 그 문장인지가 마커 종류에 따라 흔들린다.
     */
    @Test
    fun `원문 범위는 읽을 내용의 시작과 끝을 가리킨다`() {
        val raw = "**정말 중요한** 이야기입니다."
        val parsed = parseScript(raw)

        assertEquals(1, parsed.sentences.size)
        val slice = raw.substring(
            parsed.sentences[0].sourceRange.first,
            parsed.sentences[0].sourceRange.last + 1,
        )
        assertTrue("앞 마커는 범위 밖이어야 한다: $slice", slice.startsWith("정말"))
        assertTrue("문장 끝까지 닿아야 한다: $slice", slice.endsWith("이야기입니다."))
        assertTrue("사이에 낀 마커는 범위 안에 남는다: $slice", slice.contains("**"))
    }
}
