package dev.uthem.scriptplayer.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 공유받은 글의 미리보기.
 *
 * 순수 함수라 안드로이드 없이 돈다. 확인 화면이 무엇을 보여줄지가 여기서 정해진다.
 */
class SharePreviewTest {

    @Test
    fun `제목과 화자와 문장 수를 낸다`() {
        val preview = sharePreviewOf(
            """
            # 이벤트 루프 이해하기

            **진행자**: 안녕하세요.
            **게스트**: 반갑습니다.
            **진행자**: 시작할까요?
            **게스트**: 네.
            """.trimIndent(),
        )

        assertEquals("이벤트 루프 이해하기", preview.title)
        assertEquals(listOf("진행자", "게스트"), preview.speakers)
        assertEquals(5, preview.sentenceCount)
        assertTrue(preview.hasSomethingToRead)
    }

    @Test
    fun `앞 문장 셋만 미리 보여준다`() {
        val preview = sharePreviewOf("하나. 둘. 셋. 넷. 다섯.")

        assertEquals(listOf("하나.", "둘.", "셋."), preview.opening)
    }

    /**
     * 머리글도 읽히는 문장이라 첫 줄이 제목과 똑같이 나온다. 제목은 바로 위 카드에
     * 이미 있으니 미리보기에서는 뺀다 — 다만 읽히는 문장 수에서는 빼지 않는다.
     */
    @Test
    fun `제목과 같은 첫 문장은 미리보기에서 뺀다`() {
        val preview = sharePreviewOf("# 이벤트 루프\n\n하나. 둘. 셋. 넷.")

        assertEquals("이벤트 루프", preview.title)
        assertEquals(listOf("하나.", "둘.", "셋."), preview.opening)
        assertEquals(5, preview.sentenceCount)
    }

    @Test
    fun `읽을 것이 없으면 담을 수 없다고 알린다`() {
        val preview = sharePreviewOf("```\nconsole.log('a')\n```")

        assertFalse(preview.hasSomethingToRead)
        assertEquals(0, preview.sentenceCount)
        assertTrue(preview.opening.isEmpty())
    }

    @Test
    fun `화자가 있으면 요약에 적는다`() {
        val preview = sharePreviewOf("진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷.")

        assertEquals("4문장 · 화자 진행자·게스트", preview.summaryLine())
    }

    @Test
    fun `화자가 없으면 문장 수만 적는다`() {
        val preview = sharePreviewOf("그냥 서술입니다. 화자가 없습니다.")

        assertEquals("2문장", preview.summaryLine())
    }

    @Test
    fun `원문을 그대로 들고 있는다`() {
        // 담을 때 저장하는 것은 정리된 글이 아니라 원문이다 — 파서를 고치면 함께 나아진다
        val raw = "# 제목\n\n진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷."

        assertEquals(raw, sharePreviewOf(raw).raw)
    }
}
