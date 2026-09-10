package dev.uthem.scriptplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    /**
     * 목록. 최근에 손댄 것이 위로 온다.
     *
     * 열을 골라 받는다 — `SELECT *` 로 두면 본문까지 딸려 와 목록이 무거워진다.
     */
    @Query(
        """
        SELECT id, title, sentenceCount, updatedAt, lastSentenceIndex
        FROM scripts ORDER BY updatedAt DESC
        """,
    )
    fun observeSummaries(): Flow<List<ScriptSummary>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun find(id: String): ScriptEntity?

    @Insert
    suspend fun insert(script: ScriptEntity)

    @Query("UPDATE scripts SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: String, title: String, now: Long)

    @Query(
        """
        UPDATE scripts SET raw = :raw, sentenceCount = :sentenceCount, updatedAt = :now
        WHERE id = :id
        """,
    )
    suspend fun updateText(id: String, raw: String, sentenceCount: Int, now: Long)

    /**
     * 이어듣기 지점만 적는다.
     *
     * `updatedAt` 을 건드리지 않는다 — 듣기만 해도 목록 순서가 바뀌면, 어제 넣은 대본을
     * 잠깐 들었다는 이유로 오늘 넣은 것보다 위로 올라간다.
     */
    @Query(
        """
        UPDATE scripts SET lastSentenceIndex = :sentenceIndex, lastPositionMs = :positionMs
        WHERE id = :id
        """,
    )
    suspend fun saveProgress(id: String, sentenceIndex: Int, positionMs: Long)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun delete(id: String)
}
