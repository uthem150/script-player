package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 문장 나누기와 원문 오프셋.
 *
 * 오프셋이 맞는지는 눈으로 확인할 수 없어서, **원문에서 그 범위를 잘라내 문장과 견주는**
 * 방식으로 단언한다. 마크다운을 벗기고 나면 잘라낸 조각에 기호가 남을 수 있으니,
 * 기호가 없는 평문에서는 정확히 같아야 하고 그 밖에는 문장이 조각 안에 담겨야 한다.
 */
class SentenceSplitTest {

    @Test
    fun `종결부호로 문장을 나눈다`() {
        val parsed = parseScript("첫 문장입니다. 둘째 문장입니다. 셋째는 물음표인가요?")

        assertEquals(
            listOf("첫 문장입니다.", "둘째 문장입니다.", "셋째는 물음표인가요?"),
            parsed.sentences.map { it.text },
        )
    }

    @Test
    fun `줄바꿈은 언제나 경계다`() {
        val parsed = parseScript("종결부호가 없는 줄\n다음 줄")

        assertEquals(listOf("종결부호가 없는 줄", "다음 줄"), parsed.sentences.map { it.text })
    }

    @Test
    fun `빈 줄과 공백만 있는 줄은 버린다`() {
        val parsed = parseScript("첫 줄\n\n   \n둘째 줄")

        assertEquals(listOf("첫 줄", "둘째 줄"), parsed.sentences.map { it.text })
    }

    @Test
    fun `문장 번호는 0 부터 순서대로 붙는다`() {
        val parsed = parseScript("하나. 둘. 셋.")

        assertEquals(listOf(0, 1, 2), parsed.sentences.map { it.index })
    }

    @Test
    fun `평문에서는 원문 범위를 잘라내면 문장과 정확히 같다`() {
        val raw = "첫 문장입니다. 둘째 문장입니다.\n셋째 줄입니다."
        val parsed = parseScript(raw)

        // 문장이 0개면 아래 루프가 한 번도 돌지 않아 무엇도 지키지 못한다
        assertEquals(3, parsed.sentences.size)
        parsed.sentences.forEach { sentence ->
            assertEquals(
                "문장 ${sentence.index} 의 원문 범위가 어긋난다",
                sentence.text,
                raw.substring(sentence.sourceRange.first, sentence.sourceRange.last + 1),
            )
        }
    }

    @Test
    fun `원문 범위는 앞 문장보다 뒤에 있고 원문을 벗어나지 않는다`() {
        val raw = "하나. 둘. 셋.\n넷. 다섯."
        val parsed = parseScript(raw)

        assertTrue("문장이 나오지 않았다", parsed.sentences.isNotEmpty())
        var previousEnd = -1
        parsed.sentences.forEach { sentence ->
            assertTrue(
                "문장 ${sentence.index} 의 시작이 앞 문장 끝보다 앞이다",
                sentence.sourceRange.first > previousEnd,
            )
            assertTrue(
                "문장 ${sentence.index} 가 원문을 벗어난다",
                sentence.sourceRange.last < raw.length,
            )
            previousEnd = sentence.sourceRange.last
        }
    }
}
