package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 화자 라벨 검출.
 *
 * 핵심은 **오검출을 막는 규칙**이다. 줄머리의 `이름:` 을 전부 화자로 인정하면
 * `참고:`, `결론:`, `주의:` 가 화자가 되어 목소리가 배정되고, 그 뒤 문장이 엉뚱한
 * 사람 목소리로 읽힌다. 같은 라벨이 **두 번 이상** 나올 때만 화자로 본다.
 */
class SpeakerDetectionTest {

    @Test
    fun `두 번 이상 나온 라벨을 화자로 인정한다`() {
        val parsed = parseScript(
            """
            진행자: 안녕하세요.
            게스트: 반갑습니다.
            진행자: 오늘 주제는 무엇인가요?
            게스트: 이벤트 루프입니다.
            """.trimIndent(),
        )

        assertEquals(listOf("진행자", "게스트"), parsed.speakers.map { it.label })
    }

    @Test
    fun `한 번만 나온 라벨은 화자가 아니다`() {
        val parsed = parseScript(
            """
            진행자: 안녕하세요.
            진행자: 오늘은 이벤트 루프입니다.
            참고: 이 내용은 브라우저 기준입니다.
            """.trimIndent(),
        )

        assertEquals(listOf("진행자"), parsed.speakers.map { it.label })
        // '참고:' 는 라벨이 아니므로 콜론까지 그대로 읽어야 한다
        assertTrue(
            "참고 줄이 그대로 남아야 한다: ${parsed.sentences.map { it.text }}",
            parsed.sentences.any { it.text.startsWith("참고:") },
        )
    }

    @Test
    fun `화자 라벨은 읽지 않는다`() {
        val parsed = parseScript("진행자: 안녕하세요.\n게스트: 반갑습니다.\n진행자: 시작할까요?\n게스트: 네.")

        assertEquals(
            listOf("안녕하세요.", "반갑습니다.", "시작할까요?", "네."),
            parsed.sentences.map { it.text },
        )
    }

    @Test
    fun `굵게 표시한 라벨도 검출한다`() {
        val parsed = parseScript("**진행자**: 안녕하세요.\n**게스트**: 반갑습니다.\n**진행자**: 네.\n**게스트**: 네.")

        assertEquals(listOf("진행자", "게스트"), parsed.speakers.map { it.label })
        assertEquals(listOf("안녕하세요.", "반갑습니다.", "네.", "네."), parsed.sentences.map { it.text })
    }

    @Test
    fun `전각 콜론도 받는다`() {
        val parsed = parseScript("진행자： 안녕하세요.\n게스트： 반갑습니다.\n진행자： 네.\n게스트： 네.")

        assertEquals(listOf("진행자", "게스트"), parsed.speakers.map { it.label })
    }

    @Test
    fun `문장이 화자에 붙는다`() {
        val parsed = parseScript("진행자: 안녕하세요.\n게스트: 반갑습니다.\n진행자: 네.\n게스트: 네.")

        val host = parsed.speakers.first { it.label == "진행자" }
        val guest = parsed.speakers.first { it.label == "게스트" }
        assertEquals(
            listOf(host.id, guest.id, host.id, guest.id),
            parsed.sentences.map { it.speakerId },
        )
    }

    @Test
    fun `한 화자의 여러 문장이 모두 그 화자에 붙는다`() {
        val parsed = parseScript(
            """
            진행자: 첫 문장. 둘째 문장.
            게스트: 답입니다.
            진행자: 다시 질문입니다.
            게스트: 다시 답입니다.
            """.trimIndent(),
        )

        val host = parsed.speakers.first { it.label == "진행자" }
        assertEquals(host.id, parsed.sentences[0].speakerId)
        assertEquals(host.id, parsed.sentences[1].speakerId)
    }

    @Test
    fun `라벨 없는 줄은 앞 화자를 이어받는다`() {
        // 한 사람의 발언이 여러 줄로 이어지는 대본이 흔하다
        val parsed = parseScript(
            """
            진행자: 첫 줄입니다.
            줄바꿈으로 이어지는 같은 발언입니다.
            게스트: 답입니다.
            진행자: 또 질문입니다.
            게스트: 또 답입니다.
            """.trimIndent(),
        )

        val host = parsed.speakers.first { it.label == "진행자" }
        assertEquals(host.id, parsed.sentences[0].speakerId)
        assertEquals(host.id, parsed.sentences[1].speakerId)
    }

    @Test
    fun `머리글은 화자를 이어받지 않는다`() {
        val parsed = parseScript(
            """
            진행자: 첫 부분입니다.
            게스트: 답입니다.

            ## 두 번째 주제

            진행자: 다시 시작합니다.
            게스트: 좋습니다.
            """.trimIndent(),
        )

        val heading = parsed.sentences.first { it.text == "두 번째 주제" }
        assertNull("머리글에 화자가 붙었다", heading.speakerId)
    }

    @Test
    fun `너무 긴 라벨은 화자가 아니다`() {
        val parsed = parseScript(
            """
            아주아주 긴 이름을 가진 사람: 첫 줄입니다.
            아주아주 긴 이름을 가진 사람: 둘째 줄입니다.
            """.trimIndent(),
        )

        assertEquals(emptyList<Speaker>(), parsed.speakers)
    }

    @Test
    fun `문장 중간의 콜론은 라벨이 아니다`() {
        val parsed = parseScript("결론은 이렇습니다: 콜 스택이 비어야 합니다.")

        assertEquals(emptyList<Speaker>(), parsed.speakers)
        assertEquals(
            listOf("결론은 이렇습니다: 콜 스택이 비어야 합니다."),
            parsed.sentences.map { it.text },
        )
    }

    @Test
    fun `화자 없는 대본은 화자 목록이 비어 있다`() {
        val parsed = parseScript("그냥 서술만 있는 글입니다. 화자가 없습니다.")

        assertEquals(emptyList<Speaker>(), parsed.speakers)
        assertTrue(parsed.sentences.all { it.speakerId == null })
    }

    @Test
    fun `화자 id 는 라벨을 고쳐도 유지되도록 순서 기반이다`() {
        val parsed = parseScript("진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷.")

        assertEquals(listOf("s0", "s1"), parsed.speakers.map { it.id })
    }

    @Test
    fun `제목은 첫 머리글에서 가져온다`() {
        val parsed = parseScript("# 이벤트 루프 한 번에 이해하기\n\n진행자: 안녕하세요.")

        assertEquals("이벤트 루프 한 번에 이해하기", parsed.title)
    }

    @Test
    fun `머리글이 없으면 첫 문장 앞부분을 제목으로 쓴다`() {
        val parsed = parseScript("안녕하세요. 오늘은 이벤트 루프입니다.")

        assertEquals("안녕하세요.", parsed.title)
    }

    /**
     * 한 번만 말하는 화자는 라벨이 읽힌다 — 규칙의 대가다.
     *
     * 두 번 이상 반복을 요구하는 이유가 `참고:`·`결론:` 오검출을 막는 것이고, 그 대가로
     * 한 줄만 말하는 사람의 이름이 소리로 나온다. 세 사람 중 한 명이 한 마디만 하는 대본에서
     * 일어나는데, 반대쪽(모든 콜론을 화자로 보기)은 `참고:` 에 다른 목소리를 배정해
     * 더 이상하게 들린다. 알고 택한 쪽이므로 테스트로 굳혀 둔다.
     */
    @Test
    fun `한 번만 말한 사람의 라벨은 벗기지 않는다`() {
        val parsed = parseScript("진행자: 안녕하세요.")

        assertEquals(emptyList<Speaker>(), parsed.speakers)
        assertEquals(listOf("진행자: 안녕하세요."), parsed.sentences.map { it.text })
    }
}
