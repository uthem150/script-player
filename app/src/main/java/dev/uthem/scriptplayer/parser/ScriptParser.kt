package dev.uthem.scriptplayer.parser

/**
 * 대본 원문을 문장 목록으로 바꾼다.
 *
 * 원문의 문자 위치를 처음부터 끝까지 들고 다닌다. 마크다운을 벗기며 문자열을 치환하면
 * 위치가 밀려 원문으로 돌아갈 길이 사라진다. 나중에 붙일 수 있는 성질이 아니다.
 */
fun parseScript(raw: String): ParsedScript {
    val sentences = keptLines(raw)
        .flatMap { line -> splitLine(clean(line)) }
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

/**
 * 마크다운을 벗긴 글자와, 글자마다의 원문 위치.
 *
 * `origin[i]` 는 `text[i]` 가 원문에서 있던 자리다. 이 배열이 있어야 정리된 글자에서
 * 원문으로 돌아갈 수 있다.
 */
private class Cleaned(val text: String, val origin: IntArray)

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

// ── 읽지 않을 덩이 버리기 ────────────────────────────────────────────────────

/**
 * 코드 블록과 표를 통째로 버린 줄 목록.
 *
 * 코드를 읽히면 "콘솔 점 로그 열린괄호 따옴표" 가 나오고, 표는 셀을 이어 읽어도 뜻이
 * 통하지 않는다. 화면에는 보여줄 수 있어도 소리로는 버리는 것이 맞다.
 */
private fun keptLines(raw: String): List<SourceLine> {
    val kept = mutableListOf<SourceLine>()
    var openFence: String? = null

    sourceLines(raw).forEach { line ->
        val trimmed = line.text.trim()
        val fence = openFence
        if (fence != null) {
            /*
             * 펜스가 열려 있으면 닫힐 때까지 버린다.
             *
             * 닫히지 않고 원문이 끝나면 뒤가 전부 버려진다. 대본이 잘려 오는 일이 있고,
             * 그때 코드를 읽어 버리는 것보다 조금 덜 읽는 쪽이 낫다.
             */
            if (trimmed.startsWith(fence)) openFence = null
            return@forEach
        }

        val opener = fenceMarker(trimmed)
        // 줄이 파이프로 시작할 때만 표로 본다 — 문장 안에서도 파이프를 쓴다
        when {
            opener != null -> openFence = opener
            trimmed.startsWith("|") -> Unit
            else -> kept += line
        }
    }
    return kept
}

/** 물결 펜스는 세 개부터다 — 두 개는 취소선(`~~지운 말~~`)이다. */
private fun fenceMarker(trimmed: String): String? = when {
    trimmed.startsWith("```") -> "```"
    trimmed.startsWith("~~~") -> "~~~"
    else -> null
}

// ── 마크다운 벗기기 ──────────────────────────────────────────────────────────

private fun clean(line: SourceLine): Cleaned {
    val text = line.text
    val builder = StringBuilder()
    val origin = ArrayList<Int>(text.length)

    var at = blockMarkerEnd(text)
    while (at < text.length) {
        val char = text[at]
        when {
            // 이미지는 통째로 지운다 — 주소를 읽어 봐야 소음이고 대체 텍스트만 읽으면
            // 문장이 어그러진다
            char == '!' && text.startsWith("![", at) -> {
                val after = linkEnd(text, at + 1)
                if (after > 0) {
                    at = after
                    continue
                }
            }
            // 링크는 라벨만 남긴다
            char == '[' -> {
                val label = linkLabel(text, at)
                if (label != null) {
                    for (index in label.labelRange) {
                        builder.append(text[index])
                        origin += line.start + index
                    }
                    at = label.after
                    continue
                }
            }
            char == '`' || char == '*' -> {
                at++
                continue
            }
            char == '~' && text.startsWith("~~", at) -> {
                at += 2
                continue
            }
            char == '_' && isEmphasisUnderscore(text, at) -> {
                at++
                continue
            }
        }
        builder.append(char)
        origin += line.start + at
        at++
    }
    return Cleaned(builder.toString(), origin.toIntArray())
}

/**
 * 줄머리의 블록 마커가 끝나는 자리.
 *
 * 인용 안에 리스트가 들어오는 식으로 겹칠 수 있어 몇 번 되풀이한다.
 */
private fun blockMarkerEnd(text: String): Int {
    var start = 0
    repeat(3) {
        val next = oneBlockMarkerEnd(text, start)
        if (next == start) return start
        start = next
    }
    return start
}

private fun oneBlockMarkerEnd(text: String, from: Int): Int {
    var at = from
    while (at < text.length && text[at].isWhitespace()) at++
    if (at >= text.length) return at

    // 머리글 — # 은 여섯 개까지, 뒤에 공백이 와야 한다
    if (text[at] == '#') {
        var hashEnd = at
        while (hashEnd < text.length && text[hashEnd] == '#') hashEnd++
        val hashes = hashEnd - at
        if (hashes <= 6 && (hashEnd >= text.length || text[hashEnd].isWhitespace())) {
            return skipSpaces(text, hashEnd)
        }
    }

    // 인용
    if (text[at] == '>') return skipSpaces(text, at + 1)

    // 글머리표 — 뒤에 공백이 와야 한다. 없으면 그냥 붙임표나 곱셈표다
    if (text[at] in "-*+" && text.getOrNull(at + 1)?.isWhitespace() == true) {
        return skipSpaces(text, at + 1)
    }

    // 번호 목록
    var digitEnd = at
    while (digitEnd < text.length && text[digitEnd].isDigit()) digitEnd++
    if (digitEnd > at &&
        text.getOrNull(digitEnd) == '.' &&
        text.getOrNull(digitEnd + 1)?.isWhitespace() == true
    ) {
        return skipSpaces(text, digitEnd + 1)
    }

    return at
}

private fun skipSpaces(text: String, from: Int): Int {
    var at = from
    while (at < text.length && text[at].isWhitespace()) at++
    return at
}

private class LinkLabel(val labelRange: IntRange, val after: Int)

/** `[라벨](주소)` 를 만나면 라벨 자리와 닫힌 다음 자리를 낸다. 아니면 null. */
private fun linkLabel(text: String, at: Int): LinkLabel? {
    val bracketEnd = text.indexOf(']', at + 1)
    if (bracketEnd < 0 || !text.startsWith("](", bracketEnd)) return null
    val parenEnd = text.indexOf(')', bracketEnd + 2)
    if (parenEnd < 0) return null
    if (bracketEnd == at + 1) return LinkLabel(IntRange.EMPTY, parenEnd + 1)
    return LinkLabel(at + 1 until bracketEnd, parenEnd + 1)
}

/** `![alt](주소)` 가 닫힌 다음 자리. 아니면 -1. */
private fun linkEnd(text: String, bracketAt: Int): Int =
    linkLabel(text, bracketAt)?.after ?: -1

/**
 * 이 밑줄이 강조 표시인지 본다.
 *
 * 양옆이 모두 글자나 숫자면 `snake_case` 이름의 일부다 — 그것까지 벗기면
 * `my_var` 가 `myvar` 로 읽혀 무슨 말인지 알 수 없게 된다.
 */
private fun isEmphasisUnderscore(text: String, at: Int): Boolean {
    val before = text.getOrNull(at - 1)
    val after = text.getOrNull(at + 1)
    return before?.isLetterOrDigit() != true || after?.isLetterOrDigit() != true
}

// ── 문장 나누기 ──────────────────────────────────────────────────────────────

private val TERMINATORS = charArrayOf('.', '?', '!', '…')

/**
 * 한 줄을 문장으로 나눈다.
 *
 * 줄바꿈은 언제나 경계이므로 줄을 넘나드는 문장은 만들지 않는다. 종결부호는 **뒤에 공백이나
 * 줄 끝이 올 때만** 경계로 본다 — 그러지 않으면 소수점이나 약어에서 문장이 끊긴다.
 */
private fun splitLine(line: Cleaned): List<Piece> {
    val pieces = mutableListOf<Piece>()
    val text = line.text
    var chunkStart = 0

    text.forEachIndexed { at, char ->
        if (char in TERMINATORS) {
            val next = text.getOrNull(at + 1)
            if (next == null || next.isWhitespace()) {
                pieces.addTrimmed(line, chunkStart, at + 1)
                chunkStart = at + 1
            }
        }
    }
    pieces.addTrimmed(line, chunkStart, text.length)
    return pieces
}

/**
 * 앞뒤 공백을 뗀 뒤 담는다.
 *
 * 글자나 숫자가 하나도 없는 조각은 버린다. 기호만 남은 조각(구분선 잔해, 홀로 남은 마침표)을
 * 문장으로 세면 재생기가 소리 없는 자리에서 멈춰 있는 것처럼 보인다.
 */
private fun MutableList<Piece>.addTrimmed(line: Cleaned, from: Int, to: Int) {
    val text = line.text
    var begin = from
    var end = to
    while (begin < end && text[begin].isWhitespace()) begin++
    while (end > begin && text[end - 1].isWhitespace()) end--
    if (begin >= end) return

    val content = text.substring(begin, end)
    if (content.none { it.isLetterOrDigit() }) return
    this += Piece(content, line.origin[begin]..line.origin[end - 1])
}

/** 제목은 첫 문장 앞부분으로 둔다. 머리글이 있으면 그것을 쓰는 것은 다음 단계에서. */
private fun titleOf(sentences: List<Sentence>): String {
    val first = sentences.firstOrNull()?.text ?: return ""
    return if (first.length <= 40) first else first.take(39).trimEnd() + "…"
}
