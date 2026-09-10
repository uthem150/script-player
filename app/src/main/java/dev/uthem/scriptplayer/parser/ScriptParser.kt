package dev.uthem.scriptplayer.parser

/**
 * 대본 원문을 문장 목록으로 바꾼다.
 *
 * 원문의 문자 위치를 처음부터 끝까지 들고 다닌다. 마크다운을 벗기며 문자열을 치환하면
 * 위치가 밀려 원문으로 돌아갈 길이 사라진다. 나중에 붙일 수 있는 성질이 아니다.
 */
fun parseScript(raw: String): ParsedScript {
    val lines = keptLines(raw).map { describe(it) }
    val speakers = detectSpeakers(lines)
    val byLabel = speakers.associateBy { it.label }

    val sentences = mutableListOf<Sentence>()
    var current: Speaker? = null
    var pending: Speaker? = null

    lines.forEach { line ->
        /*
         * 빈 줄이 발언을 끝낸다.
         *
         * 이 규칙이 없으면 이어받기에 끝이 없다. 강의형 대본 가운데 짧은 대화 토막이
         * 있었는데, 그 라벨이 문서 끝까지 200줄을 끌고 가 모든 문장 위에 붙었다.
         * 머리글에서만 끊기니 머리글 없는 구간이 길면 그만큼 잘못 붙는다.
         *
         * 미뤄 둔 라벨([pending])은 빈 줄에 지우지 않는다 — `이름:` 다음에 빈 줄을 두고
         * 발언을 적는 대본이 있고, 그 라벨은 아래 줄에 붙어야 한다.
         */
        if (line.source.text.isBlank()) {
            current = null
            return@forEach
        }

        val speaker = byLabel[line.label]
        val from = when {
            // 머리글은 발언이 아니다. 앞 화자를 이어받으면 장 제목이 그 사람 목소리로 읽힌다
            line.isHeading -> {
                current = null
                pending = null
                line.contentStart
            }
            speaker != null -> {
                /*
                 * 라벨만 있고 내용이 없는 줄은 그 자체로 발언이 아니다.
                 * 라벨을 미뤄 두고 다음 줄에 붙인다 — 그래야 누가 말한 것인지 남는다.
                 */
                if (line.source.text.substring(line.afterLabel).isBlank()) {
                    pending = speaker
                    return@forEach
                }
                current = speaker
                pending = null
                line.afterLabel
            }
            // 라벨 없는 줄은 앞 화자를 이어받는다 — 한 발언이 여러 줄로 이어지는 대본이 흔하다
            else -> {
                pending?.let {
                    current = it
                    pending = null
                }
                line.contentStart
            }
        }

        splitLine(clean(line.source, from)).forEach { piece ->
            sentences += Sentence(
                index = sentences.size,
                text = piece.text,
                speakerId = current?.id,
                sourceRange = piece.range,
            )
        }
    }

    val heading = lines.firstOrNull { it.isHeading }
        ?.let { clean(it.source, it.contentStart).text.trim() }
        ?.takeIf { it.isNotEmpty() }

    return ParsedScript(
        title = heading?.ellipsize(60) ?: sentences.firstOrNull()?.text?.ellipsize(40) ?: "",
        speakers = speakers,
        sentences = sentences,
        titleFromHeading = heading != null,
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

/** 한 줄을 훑어 알아낸 것들. 화자를 세려면 전체를 두 번 봐야 해서 미리 담아 둔다. */
private class LineInfo(
    val source: SourceLine,
    /** 블록 마커(머리글·인용·글머리표) 다음 자리 */
    val contentStart: Int,
    val isHeading: Boolean,
    /** 화자 라벨 후보. 화자로 인정될지는 몇 번 나오는지에 달렸다 */
    val label: String?,
    /** 라벨과 콜론 다음 자리 */
    val afterLabel: Int,
)

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

// ── 화자 ─────────────────────────────────────────────────────────────────────

private fun describe(line: SourceLine): LineInfo {
    val contentStart = blockMarkerEnd(line.text)
    val hit = labelAt(line.text, contentStart)
    return LineInfo(
        source = line,
        contentStart = contentStart,
        isHeading = isHeading(line.text),
        label = hit?.label,
        afterLabel = hit?.after ?: contentStart,
    )
}

/**
 * 같은 라벨이 **두 번 이상** 나올 때만 화자로 인정한다.
 *
 * 줄머리의 `이름:` 을 전부 화자로 보면 `참고:`, `결론:`, `주의:` 가 화자가 되어 목소리가
 * 배정되고, 그 줄이 엉뚱한 사람 목소리로 읽힌다. 이 규칙 하나가 오검출을 거의 다 막는다.
 *
 * 대가는 한 줄만 말하는 사람의 이름이 소리로 나오는 것이다. 반대쪽(모든 콜론을 화자로 보기)은
 * `참고:` 에 다른 목소리를 배정해 더 이상하게 들리므로, 이쪽을 택했다.
 *
 * [Speaker.id] 는 나온 순서로 붙인다. 라벨을 그대로 쓰면 "진행자" 를 "호스트" 로 고쳤을 때
 * 음성 배정과 합성 캐시가 전부 풀린다.
 */
private fun detectSpeakers(lines: List<LineInfo>): List<Speaker> {
    val counts = mutableMapOf<String, Int>()
    val order = mutableListOf<String>()

    lines.forEach { line ->
        val label = line.label ?: return@forEach
        if (counts.put(label, (counts[label] ?: 0) + 1) == null) order += label
    }

    return order
        .filter { (counts[it] ?: 0) >= 2 }
        .mapIndexed { index, label -> Speaker(id = "s$index", label = label) }
}

private class LabelHit(val label: String, val after: Int)

/** 이름에 쓸 수 있는 글자. 쉼표나 마침표가 끼면 라벨이 아니라 문장이다. */
private fun Char.isNameChar() = isLetterOrDigit() || isWhitespace() || this in "·_-"

/**
 * 줄머리에서 `이름:` 또는 `**이름**:` 을 찾는다.
 *
 * 콜론 뒤에는 공백이나 줄 끝이 와야 한다 — `10:30` 같은 것을 라벨로 보지 않기 위해서다.
 * 이름은 12자까지. 그보다 길면 사람 이름이 아니라 문장이다.
 */
private fun labelAt(text: String, from: Int): LabelHit? {
    var at = from
    val bold = text.startsWith("**", at)
    if (bold) at += 2

    val nameStart = at
    var nameEnd = -1
    var colonAt = -1
    var scanned = 0

    while (at < text.length) {
        if (bold && text.startsWith("**", at)) {
            if (nameEnd < 0) nameEnd = at
            at += 2
            continue
        }
        val char = text[at]
        if (char == ':' || char == '：') {
            colonAt = at
            if (nameEnd < 0) nameEnd = at
            break
        }
        if (!char.isNameChar()) return null
        at++
        if (++scanned > 20) return null
    }

    if (colonAt < 0) return null
    val name = text.substring(nameStart, nameEnd).trim()
    if (name.isEmpty() || name.length > 12 || name.none { it.isLetter() }) return null

    val next = text.getOrNull(colonAt + 1)
    if (next != null && !next.isWhitespace()) return null
    return LabelHit(name, skipSpaces(text, colonAt + 1))
}

// ── 마크다운 벗기기 ──────────────────────────────────────────────────────────

private fun clean(line: SourceLine, from: Int): Cleaned {
    val text = line.text
    val builder = StringBuilder()
    val origin = ArrayList<Int>(text.length)

    var at = from
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

private fun isHeading(text: String): Boolean {
    val at = skipSpaces(text, 0)
    if (at >= text.length || text[at] != '#') return false
    var hashEnd = at
    while (hashEnd < text.length && text[hashEnd] == '#') hashEnd++
    return hashEnd - at <= 6 && (hashEnd >= text.length || text[hashEnd].isWhitespace())
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
    var at = skipSpaces(text, from)
    if (at >= text.length) return at

    // 머리글 — # 은 여섯 개까지, 뒤에 공백이 와야 한다
    if (text[at] == '#') {
        var hashEnd = at
        while (hashEnd < text.length && text[hashEnd] == '#') hashEnd++
        if (hashEnd - at <= 6 && (hashEnd >= text.length || text[hashEnd].isWhitespace())) {
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

/** 정리된 글자 안의 반열린 구간 `[from, to)`. */
private data class Span(val from: Int, val to: Int)

/**
 * TTS 엔진에 한 번에 넣을 글자 수 상한.
 *
 * 긴 입력에서 엔진이 불안정해진다. 넘으면 쉼표에서 보조 분할한다.
 */
private const val MAX_SENTENCE = 200

/**
 * 한 줄을 문장으로 나눈다.
 *
 * 줄바꿈은 언제나 경계이므로 줄을 넘나드는 문장은 만들지 않는다.
 */
private fun splitLine(line: Cleaned): List<Piece> =
    coarseSpans(line.text)
        .mapNotNull { trimSpan(line.text, it) }
        .flatMap { capLength(line.text, it) }
        .mapNotNull { toPiece(line, it) }

/**
 * 종결부호로 나눈다.
 *
 * 종결부호는 **뒤에 공백이나 줄 끝이 올 때만** 경계로 본다. 그러지 않으면 `3.14` 에서
 * 끊겨 "삼 점" 하고 멈췄다가 "일사" 로 이어진다.
 *
 * 겹친 종결부호는 묶어서 한 번만 끊는다 — `정말인가요?!` 가 두 문장이 되지 않게.
 */
private fun coarseSpans(text: String): List<Span> {
    val spans = mutableListOf<Span>()
    var start = 0
    var at = 0

    while (at < text.length) {
        if (text[at] !in TERMINATORS) {
            at++
            continue
        }
        var runEnd = at
        while (runEnd < text.length && text[runEnd] in TERMINATORS) runEnd++

        val next = text.getOrNull(runEnd)
        val isBoundary = (next == null || next.isWhitespace()) &&
            !isEllipsis(text, at, runEnd) &&
            !endsAbbreviation(text, at, runEnd)
        if (isBoundary) {
            spans += Span(start, runEnd)
            start = runEnd
        }
        at = runEnd
    }
    if (start < text.length) spans += Span(start, text.length)
    return spans
}

/**
 * 말줄임표는 경계로 보지 않는다.
 *
 * `그런데... 그게 아니었습니다.` 를 끊으면 "그런데..." 가 1초짜리 조각으로 남는다.
 * 반대로 안 끊어 문장이 길어지면 200자 상한이 받아 주므로, 안 끊는 쪽이 안전하다.
 */
private fun isEllipsis(text: String, from: Int, to: Int): Boolean =
    text.substring(from, to).contains('…') ||
        (to - from >= 2 && (from until to).all { text[it] == '.' })

private val ABBREVIATIONS = setOf(
    "e.g.", "i.e.", "vs.", "etc.", "cf.", "approx.",
    "mr.", "mrs.", "ms.", "dr.", "prof.", "st.",
    "no.", "fig.", "vol.", "ch.", "a.m.", "p.m.",
)

/** 마침표 하나로 끝나는 자리에서, 그 앞 낱말이 약어인지 본다. */
private fun endsAbbreviation(text: String, runStart: Int, runEnd: Int): Boolean {
    if (runEnd - runStart != 1 || text[runStart] != '.') return false
    var tokenStart = runStart
    while (tokenStart > 0 && !text[tokenStart - 1].isWhitespace()) tokenStart--
    return text.substring(tokenStart, runEnd).lowercase() in ABBREVIATIONS
}

private fun trimSpan(text: String, span: Span): Span? {
    var begin = span.from
    var end = span.to
    while (begin < end && text[begin].isWhitespace()) begin++
    while (end > begin && text[end - 1].isWhitespace()) end--
    return if (begin >= end) null else Span(begin, end)
}

/**
 * 200자를 넘는 조각을 쉼표에서 나눈다.
 *
 * 쉼표가 없으면 공백에서, 그것도 없으면 글자 수로 자른다. 자를 데가 없다고 그냥 두면
 * 엔진이 그 문장에서 통째로 실패해 소리가 빠진다 — 어색하게 끊기는 쪽이 낫다.
 */
private fun capLength(text: String, span: Span): List<Span> {
    if (span.to - span.from <= MAX_SENTENCE) return listOf(span)

    val spans = mutableListOf<Span>()
    var start = span.from
    while (span.to - start > MAX_SENTENCE) {
        val limit = start + MAX_SENTENCE
        val cut = cutAfter(text, start, limit, ',')
            ?: cutAfter(text, start, limit, '，')
            ?: cutAtWhitespace(text, start, limit)
            ?: limit
        spans += Span(start, cut)
        start = cut
    }
    if (start < span.to) spans += Span(start, span.to)
    return spans
}

/** [from, limit) 안의 마지막 [char] **다음** 자리. 쉼표는 앞 조각에 남긴다. */
private fun cutAfter(text: String, from: Int, limit: Int, char: Char): Int? {
    for (at in limit - 1 downTo from) {
        if (text[at] == char && at + 1 > from) return at + 1
    }
    return null
}

private fun cutAtWhitespace(text: String, from: Int, limit: Int): Int? {
    for (at in limit - 1 downTo from) {
        if (text[at].isWhitespace() && at > from) return at
    }
    return null
}

/**
 * 글자나 숫자가 하나도 없는 조각은 버린다.
 *
 * 기호만 남은 조각(구분선 잔해, 홀로 남은 마침표)을 문장으로 세면 재생기가 소리 없는
 * 자리에서 멈춰 있는 것처럼 보인다.
 */
private fun toPiece(line: Cleaned, span: Span): Piece? {
    val trimmed = trimSpan(line.text, span) ?: return null
    val content = line.text.substring(trimmed.from, trimmed.to)
    if (content.none { it.isLetterOrDigit() }) return null
    return Piece(content, line.origin[trimmed.from]..line.origin[trimmed.to - 1])
}

private fun String.ellipsize(limit: Int) =
    if (length <= limit) this else take(limit - 1).trimEnd() + "…"
