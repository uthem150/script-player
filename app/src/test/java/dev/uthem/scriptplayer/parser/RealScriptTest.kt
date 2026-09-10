package dev.uthem.scriptplayer.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 실제 AI 생성 대본으로 회귀를 막는다.
 *
 * 규칙을 하나씩 시험하는 테스트는 각각 통과해도, 진짜 대본에서 함께 작동할지는 알 수 없다.
 * 머리글 다음에 인용이 오고, 화자 라벨이 굵게 표시돼 있고, 중간에 코드 블록이 끼는 식으로
 * 겹치는 것이 실제 모습이다.
 *
 * 픽스처는 앱이 실제로 쓰는 `app/src/main/assets/sample-script.md` 를 그대로 읽는다 —
 * 테스트용 사본을 따로 두면 둘이 어긋나고, 그때 어느 쪽이 맞는지 알 수 없다.
 */
class RealScriptTest {

    private val raw: String = run {
        val file = File("src/main/assets/sample-script.md")
        assertTrue("픽스처가 없다: ${file.absolutePath}", file.isFile)
        file.readText()
    }

    private val parsed = parseScript(raw)

    @Test
    fun `제목을 첫 머리글에서 가져온다`() {
        assertEquals("자바스크립트 이벤트 루프, 한 번에 이해하기", parsed.title)
    }

    @Test
    fun `화자 둘을 검출한다`() {
        assertEquals(listOf("진행자", "게스트"), parsed.speakers.map { it.label })
    }

    /**
     * 코드 **블록**만 버린다.
     *
     * 여기 쓰는 조각들은 펜스 안에만 있는 것이어야 한다. 처음에 `setTimeout(` 을 넣었다가
     * 걸렸는데, 그것은 본문의 인라인 코드(`` `setTimeout(fn, 0)` ``)에서 온 것이었다 —
     * 백틱만 벗겨 남긴 것이고 그건 읽어야 맞다. 둘을 뭉개면 테스트가 옳은 동작을 막는다.
     */
    @Test
    fun `코드 블록이 새어 나오지 않는다`() {
        val texts = parsed.sentences.map { it.text }
        listOf("console.log", "Promise.resolve", "=>").forEach { fragment ->
            assertFalse(
                "코드 블록이 문장에 남았다: $fragment",
                texts.any { it.contains(fragment) },
            )
        }
    }

    /** 본문에 낀 인라인 코드는 읽는다. 백틱만 벗기고 내용은 남긴다. */
    @Test
    fun `인라인 코드는 백틱만 벗기고 남긴다`() {
        val texts = parsed.sentences.map { it.text }
        assertTrue(
            "인라인 코드가 통째로 사라졌다",
            texts.any { it.contains("setTimeout(fn, 0)") },
        )
        assertTrue(
            "백틱이 남았다",
            texts.none { it.contains("`") },
        )
    }

    @Test
    fun `앞머리 문장들이 순서대로 나온다`() {
        assertEquals(
            listOf(
                "자바스크립트 이벤트 루프, 한 번에 이해하기",
                "오늘은 비동기가 왜 그렇게 돌아가는지 밑바닥부터 짚어봅니다.",
                "안녕하세요.",
                "오늘은 자바스크립트의 이벤트 루프를 다뤄보겠습니다.",
            ),
            parsed.sentences.take(4).map { it.text },
        )
    }

    @Test
    fun `마크다운 기호가 남지 않는다`() {
        parsed.sentences.forEach { sentence ->
            listOf("**", "##", "`", "](").forEach { marker ->
                assertFalse(
                    "문장 ${sentence.index} 에 $marker 가 남았다: ${sentence.text}",
                    sentence.text.contains(marker),
                )
            }
        }
    }

    @Test
    fun `화자 라벨이 읽히지 않는다`() {
        parsed.sentences.forEach { sentence ->
            assertFalse(
                "문장 ${sentence.index} 가 라벨로 시작한다: ${sentence.text}",
                sentence.text.startsWith("진행자") || sentence.text.startsWith("게스트"),
            )
        }
    }

    @Test
    fun `모든 문장이 200자 이하다`() {
        parsed.sentences.forEach { sentence ->
            assertTrue(
                "문장 ${sentence.index} 가 ${sentence.text.length}자다",
                sentence.text.length <= 200,
            )
        }
    }

    @Test
    fun `모든 문장에 읽을 내용이 있다`() {
        parsed.sentences.forEach { sentence ->
            assertTrue(
                "문장 ${sentence.index} 에 읽을 것이 없다: '${sentence.text}'",
                sentence.text.any { it.isLetterOrDigit() },
            )
        }
    }

    @Test
    fun `원문 범위가 순서대로이고 원문을 벗어나지 않는다`() {
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

    @Test
    fun `화자 없는 문장은 머리글과 인용뿐이다`() {
        val orphans = parsed.sentences.filter { it.speakerId == null }.map { it.text }

        // 머리글 하나와 인용 하나가 대본 앞머리에 있다. 그 밖에 화자를 못 찾은 문장이
        // 있으면 라벨을 이어받는 규칙이 새는 것이다.
        assertEquals(
            listOf(
                "자바스크립트 이벤트 루프, 한 번에 이해하기",
                "오늘은 비동기가 왜 그렇게 돌아가는지 밑바닥부터 짚어봅니다.",
            ),
            orphans,
        )
    }

    /**
     * 문장 수를 못 박아 둔다.
     *
     * 규칙을 고쳤을 때 의도한 변화인지 아닌지 이 숫자가 먼저 알려 준다.
     * 바뀌면 실제 문장 목록을 보고 나아졌는지 판단한 뒤 이 숫자를 고친다.
     */
    @Test
    fun `문장 수가 유지된다`() {
        assertEquals(41, parsed.sentences.size)
    }
}
