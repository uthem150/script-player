package dev.uthem.scriptplayer.parser

/**
 * 대본 원문을 문장 목록으로 바꾼다.
 *
 * 원문의 문자 위치를 처음부터 끝까지 들고 다닌다. 마크다운을 벗기며 문자열을 치환하면
 * 위치가 밀려, 단어를 탭했을 때 엉뚱한 데서 재생된다.
 */
fun parseScript(raw: String): ParsedScript {
    val sentences = sourceLines(raw)
        .flatMap { splitLine(it) }
        .mapIndexed { index, piece ->
            Sentence(
                index = index,
                text = piece.text,
                speakerId = null,
                sourceRange = piece.range,
            )
        }

    return ParsedScript(
        title = titleOf(sentences),
        speakers = emptyList(),
        sentences = sentences,
    )
}

/** 정리된 글자와, 그것이 원문에서 차지하는 범위. */
private data class Piece(val text: String, val range: IntRange)

/** 원문 한 줄과 그 줄이 원문에서 시작하는 위치. */
private data class SourceLine(val text: String, val start: Int)

private fun sourceLines(raw: String): List<SourceLine> {
    val lines = mutableListOf<SourceLine>()
    var start = 0
    while (true) {
        val newline = raw.indexOf('\n', start)
        val end = if (newline < 0) raw.length else newline
        lines += SourceLine(raw.substring(start, end), start)
        if (newline < 0) break
        start = newline + 1
    }
    return lines
}

private val TERMINATORS = charArrayOf('.', '?', '!', '…')

/**
 * 한 줄을 문장으로 나눈다.
 *
 * 줄바꿈은 언제나 경계이므로 줄을 넘나드는 문장은 만들지 않는다. 종결부호는 **뒤에 공백이나
 * 줄 끝이 올 때만** 경계로 본다 — 그러지 않으면 소수점이나 약어에서 문장이 끊긴다.
 */
private fun splitLine(line: SourceLine): List<Piece> {
    val pieces = mutableListOf<Piece>()
    val text = line.text
    var chunkStart = 0

    text.forEachIndexed { at, char ->
        if (char in TERMINATORS) {
            val next = text.getOrNull(at + 1)
            if (next == null || next.isWhitespace()) {
                pieces.addTrimmed(text, chunkStart, at + 1, line.start)
                chunkStart = at + 1
            }
        }
    }
    pieces.addTrimmed(text, chunkStart, text.length, line.start)
    return pieces
}

/**
 * 앞뒤 공백을 뗀 뒤 담는다.
 *
 * 글자나 숫자가 하나도 없는 조각은 버린다. 기호만 남은 조각(구분선 잔해, 홀로 남은 마침표)을
 * 문장으로 세면 재생기가 소리 없는 자리에서 멈춰 있는 것처럼 보인다.
 */
private fun MutableList<Piece>.addTrimmed(text: String, from: Int, to: Int, lineStart: Int) {
    var begin = from
    var end = to
    while (begin < end && text[begin].isWhitespace()) begin++
    while (end > begin && text[end - 1].isWhitespace()) end--
    if (begin >= end) return

    val content = text.substring(begin, end)
    if (content.none { it.isLetterOrDigit() }) return
    this += Piece(content, lineStart + begin..lineStart + end - 1)
}

/** 제목은 첫 문장 앞부분으로 둔다. 머리글이 있으면 그것을 쓰는 것은 다음 단계에서. */
private fun titleOf(sentences: List<Sentence>): String {
    val first = sentences.firstOrNull()?.text ?: return ""
    return if (first.length <= 40) first else first.take(39).trimEnd() + "…"
}
