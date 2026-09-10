package dev.uthem.scriptplayer.ui.share

import dev.uthem.scriptplayer.parser.parseScript

/**
 * 공유받은 글을 담기 전에 보여줄 것.
 *
 * 순수 함수로 만든다 — 안드로이드 없이 테스트할 수 있고, 화면은 이 값을 그리기만 한다.
 */
data class SharePreview(
    val raw: String,
    val title: String,
    val speakers: List<String>,
    val sentenceCount: Int,
    /** 앞 문장 몇 개. 무엇이 들어왔는지 눈으로 확인시키는 용도다 */
    val opening: List<String>,
) {
    val hasSomethingToRead: Boolean get() = sentenceCount > 0
}

private const val OPENING_LINES = 3

/**
 * 공유받은 글을 훑어 미리보기를 만든다.
 *
 * 담기 전에 확인을 받는 이유는, 잘못 던진 글이 조용히 쌓이는 것을 막기 위해서다.
 * 메신저에서 텍스트를 공유할 때 엉뚱한 것을 고르는 일이 흔하고, 그것이 아무 말 없이
 * 보관함에 들어가면 나중에 왜 있는지 알 수 없는 항목이 남는다.
 */
fun sharePreviewOf(raw: String): SharePreview {
    val parsed = parseScript(raw)
    val texts = parsed.sentences.map { it.text }
    return SharePreview(
        raw = raw,
        title = parsed.title,
        speakers = parsed.speakers.map { it.label },
        sentenceCount = texts.size,
        /*
         * 머리글에서 온 제목은 미리보기에서 한 번 뺀다.
         *
         * 머리글도 읽히는 문장이라 첫 줄이 제목과 똑같이 나오는데, 제목은 바로 위 카드에
         * 이미 있다. 세 줄 중 하나를 이미 아는 정보에 쓰는 대신 다음 문장을 보여준다.
         *
         * **머리글에서 왔을 때만** 뺀다. 머리글이 없으면 첫 문장 앞부분이 제목이 되므로
         * 같아 보여도 그것은 실제 내용이다 — 지우면 읽을 것을 잃는다.
         * 읽히는 문장 수에서는 빼지 않는다. 실제로 읽기 때문이다.
         */
        opening = texts
            .let { if (parsed.titleFromHeading) it.drop(1) else it }
            .take(OPENING_LINES),
    )
}

/** 카드 아래 한 줄 요약. */
fun SharePreview.summaryLine(): String {
    val length = "${sentenceCount}문장"
    return if (speakers.isEmpty()) length else "$length · 화자 ${speakers.joinToString("·")}"
}
