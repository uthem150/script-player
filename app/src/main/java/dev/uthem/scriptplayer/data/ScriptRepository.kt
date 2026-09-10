package dev.uthem.scriptplayer.data

import dev.uthem.scriptplayer.parser.ParsedScript
import dev.uthem.scriptplayer.parser.parseScript
import kotlinx.coroutines.flow.Flow

/** 대본을 담을 때의 결과. 빈 것을 조용히 담지 않는다. */
sealed interface AddResult {
    data class Added(val id: String) : AddResult

    /** 글자는 있었지만 읽을 문장이 하나도 없었다 — 코드 블록만 붙여넣은 경우가 이렇다. */
    data object NothingToRead : AddResult
}

/**
 * 보관함.
 *
 * 파싱은 여기서만 한다 — 화면은 문장을 어떻게 쪼개는지 몰라도 되고, 파서를 고쳐도
 * 화면이 바뀌지 않는다.
 *
 * [now] 와 [newId] 를 주입받는다. 시각과 난수를 직접 부르면 테스트가 그 값을 못 잡아,
 * "담고 나서 목록에 뜨는가" 같은 것만 확인하고 정렬은 확인하지 못한다.
 */
class ScriptRepository(
    private val dao: ScriptDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun observeLibrary(): Flow<List<ScriptSummary>> = dao.observeSummaries()

    /**
     * 붙여넣거나 공유받은 글을 담는다.
     *
     * 읽을 문장이 없으면 담지 않는다. 조용히 담으면 보관함에 소리 안 나는 항목이 쌓이고,
     * 눌러 봐야 아무 일도 없는 이유를 알 수 없다.
     */
    suspend fun add(raw: String): AddResult {
        val parsed = parseScript(raw)
        if (parsed.sentences.isEmpty()) return AddResult.NothingToRead

        val timestamp = now()
        val id = newId()
        dao.insert(
            ScriptEntity(
                id = id,
                title = parsed.title,
                raw = raw,
                sentenceCount = parsed.sentences.size,
                createdAt = timestamp,
                updatedAt = timestamp,
                lastSentenceIndex = 0,
                lastPositionMs = 0,
            ),
        )
        return AddResult.Added(id)
    }

    suspend fun load(id: String): ParsedScript? = dao.find(id)?.let { parseScript(it.raw) }

    /** 어디까지 들었는지. 없는 대본이면 처음. */
    suspend fun progressOf(id: String): Progress =
        dao.find(id)?.let { Progress(it.lastSentenceIndex, it.lastPositionMs) } ?: Progress(0, 0)

    data class Progress(val sentenceIndex: Int, val positionMs: Long)

    suspend fun rename(id: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        dao.rename(id, trimmed, now())
    }

    /**
     * 본문을 고친다.
     *
     * **제목은 건드리지 않는다.** 다시 파싱하면 첫 머리글에서 제목이 나오는데, 사용자가
     * 손으로 고친 제목을 그것으로 덮으면 고친 것이 사라진다. 제목은 [rename] 으로만 바뀐다.
     */
    suspend fun updateText(id: String, raw: String): AddResult {
        val parsed = parseScript(raw)
        if (parsed.sentences.isEmpty()) return AddResult.NothingToRead
        dao.updateText(id, raw, parsed.sentences.size, now())
        return AddResult.Added(id)
    }

    suspend fun saveProgress(id: String, sentenceIndex: Int, positionMs: Long) =
        dao.saveProgress(id, sentenceIndex, positionMs)

    suspend fun delete(id: String) = dao.delete(id)
}
