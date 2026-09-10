package dev.uthem.scriptplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 보관함에 담긴 대본 한 편.
 *
 * [raw] 는 원문이고, 이것이 유일한 원본이다. 문장 목록은 저장하지 않고 필요할 때
 * 다시 파싱한다 — 파서를 고치면 이미 담긴 대본도 함께 나아진다. 저장해 두면 옛 규칙으로
 * 쪼갠 문장이 남아, 고친 파서와 다르게 읽힌다.
 */
@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val raw: String,
    /**
     * 문장 수. 원문에서 다시 셀 수 있지만 목록에 필요해 함께 둔다.
     * 목록을 그릴 때마다 수십 편을 파싱하면 스크롤이 걸린다.
     */
    val sentenceCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** 이어듣기 — 몇 번째 문장의 어디까지 들었는지 */
    val lastSentenceIndex: Int,
    val lastPositionMs: Long,
)

/**
 * 목록에 그릴 만큼만.
 *
 * `raw` 를 뺀 것이 요점이다. 대본 하나가 수십 KB 이고 보관함에 수십 편이 쌓이면,
 * 목록을 열 때마다 본문 전체를 메모리로 끌어올릴 이유가 없다.
 */
data class ScriptSummary(
    val id: String,
    val title: String,
    val sentenceCount: Int,
    val updatedAt: Long,
    val lastSentenceIndex: Int,
) {
    /** 들은 진도. 문장이 없으면 0 — 나누기 전에 막는다. */
    val progress: Float
        get() = if (sentenceCount <= 0) 0f else (lastSentenceIndex.toFloat() / sentenceCount).coerceIn(0f, 1f)
}
