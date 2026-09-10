package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 읽어 봐야 소음인 덩이는 통째로 버린다.
 *
 * 코드 블록을 읽히면 "콘솔 점 로그 열린괄호 따옴표 하나 따옴표 닫힌괄호" 가 나온다.
 * 표는 셀을 이어 읽어도 뜻이 통하지 않는다. 둘 다 화면에서는 보여줄 수 있지만
 * 소리로는 버리는 것이 맞다.
 */
class BlockRemovalTest {

    private fun textsOf(raw: String) = parseScript(raw).sentences.map { it.text }

    @Test
    fun `펜스 코드 블록을 통째로 버린다`() {
        val raw = """
            앞 문장입니다.
            ```javascript
            console.log('1')
            setTimeout(() => console.log('2'), 0)
            ```
            뒤 문장입니다.
        """.trimIndent()

        assertEquals(listOf("앞 문장입니다.", "뒤 문장입니다."), textsOf(raw))
    }

    @Test
    fun `언어 표시가 없는 코드 블록도 버린다`() {
        val raw = "앞.\n```\nsome code\n```\n뒤."

        assertEquals(listOf("앞.", "뒤."), textsOf(raw))
    }

    @Test
    fun `물결 펜스도 코드 블록이다`() {
        val raw = "앞.\n~~~\nsome code\n~~~\n뒤."

        assertEquals(listOf("앞.", "뒤."), textsOf(raw))
    }

    @Test
    fun `닫히지 않은 코드 블록은 끝까지 버린다`() {
        // 대본이 잘려 오는 일이 있다. 열린 채 끝나면 뒤는 전부 코드로 본다 —
        // 코드를 읽어 버리는 것보다 조금 덜 읽는 쪽이 낫다
        val raw = "앞 문장입니다.\n```\ncode\nmore code"

        assertEquals(listOf("앞 문장입니다."), textsOf(raw))
    }

    @Test
    fun `표를 통째로 버린다`() {
        val raw = """
            앞 문장입니다.

            | 항목 | 값 |
            | --- | --- |
            | 콜 스택 | 비어야 다음 일이 시작된다 |
            | 마이크로태스크 | 태스크보다 먼저 |

            뒤 문장입니다.
        """.trimIndent()

        assertEquals(listOf("앞 문장입니다.", "뒤 문장입니다."), textsOf(raw))
    }

    @Test
    fun `문장 안의 파이프는 표가 아니다`() {
        // 줄이 파이프로 시작할 때만 표로 본다
        assertEquals(
            listOf("a | b 처럼 파이프를 쓰기도 합니다."),
            textsOf("a | b 처럼 파이프를 쓰기도 합니다."),
        )
    }

    @Test
    fun `코드 블록 안의 종결부호가 문장을 만들지 않는다`() {
        val raw = "앞.\n```\nconsole.log('a'). foo. bar.\n```\n뒤."
        val texts = textsOf(raw)

        assertEquals(listOf("앞.", "뒤."), texts)
        assertFalse("코드가 새어 나왔다: $texts", texts.any { it.contains("console") })
    }

    @Test
    fun `코드 블록을 버려도 뒤 문장의 원문 범위는 맞는다`() {
        val raw = "앞.\n```\ncode\n```\n뒤 문장입니다."
        val parsed = parseScript(raw)

        val last = parsed.sentences.last()
        assertEquals(
            "뒤 문장입니다.",
            raw.substring(last.sourceRange.first, last.sourceRange.last + 1),
        )
    }
}
