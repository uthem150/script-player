package dev.uthem.scriptplayer.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 보관함.
 *
 * 메모리 데이터베이스로 돈다 — 파일을 쓰지 않으니 테스트끼리 상태가 새지 않는다.
 * 시각과 id 는 주입해서 정렬과 동일성을 실제로 확인한다. `System.currentTimeMillis` 를
 * 직접 부르면 "담으면 뜨는가" 만 볼 수 있고 순서는 볼 수 없다.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ScriptRepositoryTest {

    private lateinit var database: ScriptDatabase
    private lateinit var repository: ScriptRepository
    private var clock = 1_000L
    private var idCounter = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScriptDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScriptRepository(
            dao = database.scriptDao(),
            now = { clock },
            newId = { "id${idCounter++}" },
        )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun library() = repository.observeLibrary().first()

    @Test
    fun `담으면 목록에 뜨고 제목과 문장 수가 채워진다`() = runTest {
        val added = repository.add("# 이벤트 루프\n\n진행자: 안녕하세요.\n게스트: 반갑습니다.\n진행자: 네.\n게스트: 네.")

        assertEquals(AddResult.Added("id0"), added)
        val library = library()
        assertEquals(1, library.size)
        assertEquals("이벤트 루프", library[0].title)
        assertEquals(5, library[0].sentenceCount)
    }

    @Test
    fun `읽을 문장이 없으면 담지 않는다`() = runTest {
        // 코드 블록만 붙여넣으면 읽을 것이 하나도 남지 않는다
        val added = repository.add("```\nconsole.log('a')\n```")

        assertEquals(AddResult.NothingToRead, added)
        assertTrue("담기지 않아야 한다", library().isEmpty())
    }

    @Test
    fun `빈 글은 담지 않는다`() = runTest {
        assertEquals(AddResult.NothingToRead, repository.add("   \n\n  "))
        assertTrue(library().isEmpty())
    }

    @Test
    fun `최근에 손댄 것이 위로 온다`() = runTest {
        clock = 1_000
        repository.add("첫째 대본입니다.")
        clock = 2_000
        repository.add("둘째 대본입니다.")

        assertEquals(listOf("둘째 대본입니다.", "첫째 대본입니다."), library().map { it.title })
    }

    @Test
    fun `이름을 바꾼다`() = runTest {
        val id = (repository.add("아무 대본입니다.") as AddResult.Added).id
        clock = 5_000

        repository.rename(id, "  내가 고친 제목  ")

        assertEquals("내가 고친 제목", library()[0].title)
        assertEquals(5_000, library()[0].updatedAt)
    }

    @Test
    fun `빈 이름으로는 바꾸지 않는다`() = runTest {
        val id = (repository.add("아무 대본입니다.") as AddResult.Added).id

        repository.rename(id, "   ")

        assertEquals("아무 대본입니다.", library()[0].title)
    }

    /**
     * 본문을 고쳐도 제목은 그대로여야 한다.
     *
     * 다시 파싱하면 첫 머리글에서 제목이 나오는데, 사용자가 손으로 고친 제목을 그것으로
     * 덮으면 고친 것이 사라진다.
     */
    @Test
    fun `본문을 고쳐도 손으로 정한 제목은 유지된다`() = runTest {
        val id = (repository.add("# 원래 제목\n\n첫 문장입니다.") as AddResult.Added).id
        repository.rename(id, "내가 고친 제목")

        repository.updateText(id, "# 다른 제목\n\n첫 문장입니다. 둘째 문장입니다.")

        assertEquals("내가 고친 제목", library()[0].title)
        assertEquals(3, library()[0].sentenceCount)
    }

    /**
     * 듣기만 해도 목록 순서가 바뀌면 안 된다.
     *
     * 어제 넣은 대본을 잠깐 들었다는 이유로 오늘 넣은 것보다 위로 올라가면, 목록이
     * "최근에 넣은 것" 도 "최근에 들은 것" 도 아닌 뒤죽박죽이 된다.
     */
    @Test
    fun `이어듣기 저장은 목록 순서를 바꾸지 않는다`() = runTest {
        clock = 1_000
        val old = (repository.add("어제 넣은 대본입니다.") as AddResult.Added).id
        clock = 2_000
        repository.add("오늘 넣은 대본입니다.")

        clock = 9_000
        repository.saveProgress(old, sentenceIndex = 3, positionMs = 1_500)

        assertEquals(
            listOf("오늘 넣은 대본입니다.", "어제 넣은 대본입니다."),
            library().map { it.title },
        )
    }

    @Test
    fun `이어듣기 지점이 진도로 나온다`() = runTest {
        val id = (repository.add("하나. 둘. 셋. 넷.") as AddResult.Added).id

        repository.saveProgress(id, sentenceIndex = 2, positionMs = 0)

        assertEquals(0.5f, library()[0].progress, 0.001f)
    }

    @Test
    fun `문장이 없으면 진도는 0 이다`() {
        val summary = ScriptSummary("id", "제목", sentenceCount = 0, updatedAt = 0, lastSentenceIndex = 0)

        assertEquals(0f, summary.progress, 0f)
    }

    @Test
    fun `지운다`() = runTest {
        val id = (repository.add("지울 대본입니다.") as AddResult.Added).id

        repository.delete(id)

        assertTrue(library().isEmpty())
    }

    @Test
    fun `불러오면 파싱된 문장이 온다`() = runTest {
        val id = (repository.add("진행자: 하나.\n게스트: 둘.\n진행자: 셋.\n게스트: 넷.") as AddResult.Added).id

        val parsed = repository.load(id)

        assertNotNull(parsed)
        assertEquals(listOf("하나.", "둘.", "셋.", "넷."), parsed!!.sentences.map { it.text })
        assertEquals(listOf("진행자", "게스트"), parsed.speakers.map { it.label })
    }

    @Test
    fun `없는 것을 불러오면 null 이다`() = runTest {
        assertNull(repository.load("없는-id"))
    }
}
