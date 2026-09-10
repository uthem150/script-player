package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 화자 라벨이 어디까지 미치는가.
 *
 * 실기기에서 나온 문제. 강의형 대본 가운데 짧은 대화 토막이 있었는데,
 * 그 라벨이 문서 끝까지 200줄을 끌고 갔다 — 모든 문장 위에 `Auto-height` 가 붙었다.
 *
 * 원인은 "라벨 없는 줄은 앞 화자를 이어받는다" 규칙에 끝이 없었던 것이다.
 * 머리글에서만 끊겼는데, 머리글이 없는 구간이 길면 그만큼 잘못 붙는다.
 */
class SpeakerScopeTest {

    private fun labelsOf(raw: String) = parseScript(raw).let { parsed ->
        parsed.sentences.map { sentence ->
            sentence.speakerId?.let { id -> parsed.speakers.first { it.id == id }.label }
        }
    }

    /** 실제로 문제가 된 대본의 그 부분. */
    private val realScript = """
        약간 닭이 먼저냐 달걀이 먼저냐 같은 상황입니다.

        Virtualizer:

        "높이를 알려줘."

        Auto-height:

        "그려봐야 알아."

        Virtualizer:

        "그런데 높이를 알아야 그릴 수 있어."

        Auto-height:

        "그러니까 일단 예상값으로 그려."

        이렇게 해결합니다.

        예상값을 사용해서 먼저 그립니다.

        그리고 실제 높이를 측정합니다.
    """.trimIndent()

    @Test
    fun `빈 줄이 발언을 끝낸다`() {
        val labels = labelsOf(realScript)

        // 대화가 끝난 뒤의 서술에는 화자가 붙지 않아야 한다
        assertEquals(
            listOf("이렇게 해결합니다.", "예상값을 사용해서 먼저 그립니다.", "그리고 실제 높이를 측정합니다."),
            parseScript(realScript).sentences.takeLast(3).map { it.text },
        )
        assertEquals(listOf(null, null, null), labels.takeLast(3))
    }

    @Test
    fun `대화 앞의 서술에도 화자가 붙지 않는다`() {
        assertNull(labelsOf(realScript).first())
    }

    /** 라벨만 있는 줄은 다음 줄에 붙는다 — 그래야 누가 말한 것인지 화면에 남는다. */
    @Test
    fun `라벨만 있는 줄은 빈 줄을 건너 다음 줄에 붙는다`() {
        val parsed = parseScript(realScript)
        val quotes = parsed.sentences.filter { it.text.startsWith("\"") }

        assertEquals(4, quotes.size)
        assertEquals(
            listOf("Virtualizer", "Auto-height", "Virtualizer", "Auto-height"),
            quotes.map { sentence ->
                parsed.speakers.first { it.id == sentence.speakerId }.label
            },
        )
    }

    @Test
    fun `라벨만 있는 줄은 소리로 읽지 않는다`() {
        val texts = parseScript(realScript).sentences.map { it.text }

        assertEquals(
            "라벨이 문장으로 남았다: $texts",
            emptyList<String>(),
            texts.filter { it == "Virtualizer:" || it == "Auto-height:" },
        )
    }

    /** 빈 줄 없이 이어지는 여러 줄은 그대로 한 발언이다 — 이 규칙이 죽어서는 안 된다. */
    @Test
    fun `빈 줄이 없으면 여러 줄이 한 발언으로 이어진다`() {
        val labels = labelsOf(
            """
            진행자: 첫 줄입니다.
            이어지는 같은 발언입니다.
            게스트: 답입니다.
            진행자: 또 질문입니다.
            게스트: 또 답입니다.
            """.trimIndent(),
        )

        assertEquals(
            listOf("진행자", "진행자", "게스트", "진행자", "게스트"),
            labels,
        )
    }

    @Test
    fun `빈 줄 뒤에 새 라벨이 오면 그 화자로 이어진다`() {
        val labels = labelsOf(
            """
            진행자: 첫 발언입니다.

            게스트: 둘째 발언입니다.
            이어지는 줄입니다.

            진행자: 셋째 발언입니다.

            게스트: 넷째 발언입니다.
            """.trimIndent(),
        )

        assertEquals(listOf("진행자", "게스트", "게스트", "진행자", "게스트"), labels)
    }

    @Test
    fun `머리글도 여전히 발언을 끊는다`() {
        val labels = labelsOf(
            """
            진행자: 첫 부분입니다.
            게스트: 답입니다.
            ## 두 번째 주제
            진행자: 다시 시작합니다.
            게스트: 좋습니다.
            """.trimIndent(),
        )

        assertEquals(listOf("진행자", "게스트", null, "진행자", "게스트"), labels)
    }
}
