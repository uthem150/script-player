package dev.uthem.scriptplayer.data

import dev.uthem.scriptplayer.tts.SynthesisRequest
import dev.uthem.scriptplayer.tts.WordTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * 콘텐츠 주소 캐시.
 *
 * 안드로이드가 필요 없다 — 파일과 해시뿐이라 임시 폴더로 돈다.
 * 이 캐시가 대본 수정·화자 재배정의 재합성 범위를 정하므로, 키가 흔들리면 매번 전부
 * 다시 합성된다.
 */
class AudioCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    /**
     * 그럴듯한 시각에서 시작한다.
     *
     * 처음에 1000(1970년)으로 뒀더니 `setLastModified` 가 조용히 실패해, 파일 셋이 모두
     * 같은 시각을 갖고 정렬이 뒤섞였다. 그 바람에 최근에 쓴 것이 먼저 지워졌다.
     */
    private var clock = 1_700_000_000_000L

    private fun cache(maxBytes: Long = AudioCache.DEFAULT_MAX_BYTES) =
        AudioCache(root = folder.root, maxBytes = maxBytes, now = { clock })

    private fun writeAudio(cache: AudioCache, key: String, bytes: Int) {
        val file = cache.audioFile(key)
        file.writeBytes(ByteArray(bytes))
        // 실패하면 정렬이 무의미해지므로 조용히 지나가지 않게 한다
        assertTrue("파일 시각을 설정하지 못했다: $key", file.setLastModified(clock))
    }

    private val day = 24 * 60 * 60 * 1000L

    private val request = SynthesisRequest(text = "안녕하세요.", voiceName = "ko-local", pitch = 1.0f)

    @Test
    fun `같은 입력이면 같은 키다`() {
        val subject = cache()

        assertEquals(subject.keyOf(request, "engine-1"), subject.keyOf(request, "engine-1"))
    }

    @Test
    fun `문장이 다르면 키가 다르다`() {
        val subject = cache()

        assertNotEquals(
            subject.keyOf(request, "engine-1"),
            subject.keyOf(request.copy(text = "다른 문장입니다."), "engine-1"),
        )
    }

    /** 화자 배정을 바꾸면 그 화자 문장만 다시 합성되어야 한다 — 음성이 키에 들어가야 한다. */
    @Test
    fun `음성이 다르면 키가 다르다`() {
        val subject = cache()

        assertNotEquals(
            subject.keyOf(request, "engine-1"),
            subject.keyOf(request.copy(voiceName = "ko-other"), "engine-1"),
        )
    }

    @Test
    fun `음높이가 다르면 키가 다르다`() {
        val subject = cache()

        assertNotEquals(
            subject.keyOf(request, "engine-1"),
            subject.keyOf(request.copy(pitch = 0.88f), "engine-1"),
        )
    }

    /** 엔진이 갱신되면 같은 문장도 다르게 들릴 수 있다. */
    @Test
    fun `엔진이 다르면 키가 다르다`() {
        val subject = cache()

        assertNotEquals(subject.keyOf(request, "engine-1"), subject.keyOf(request, "engine-2"))
    }

    @Test
    fun `없으면 찾지 못한다`() {
        assertNull(cache().find("없는-키"))
    }

    @Test
    fun `소리가 없으면 찾지 못한다`() {
        val subject = cache()
        // RIFF 헤더만 있고 소리가 없는 파일 — 합성이 중간에 끊긴 흔적이다
        subject.audioFile("key").writeBytes(ByteArray(44))

        assertNull(subject.find("key"))
    }

    @Test
    fun `담아 두면 찾는다`() {
        val subject = cache()
        writeAudio(subject, "key", 5_000)
        subject.storeTimings("key", listOf(WordTiming(0, 2, 100), WordTiming(3, 5, 400)))

        val found = subject.find("key")

        assertEquals(2, found?.wordTimings?.size)
        assertEquals(WordTiming(3, 5, 400), found?.wordTimings?.get(1))
    }

    @Test
    fun `타이밍이 없어도 소리는 찾는다`() {
        val subject = cache()
        writeAudio(subject, "key", 5_000)

        val found = subject.find("key")

        assertTrue("소리는 있어야 한다", found?.audio?.isFile == true)
        assertTrue("타이밍은 비어 있어야 한다", found?.wordTimings?.isEmpty() == true)
    }

    /**
     * 절반만 맞는 표로 단어를 짚으면 엉뚱한 자리에서 재생된다 — 없는 것보다 나쁘다.
     * 없으면 글자 비율로 추정하고 하이라이트를 끄면 되지만, 틀린 표는 틀린 채로 쓰인다.
     */
    @Test
    fun `망가진 타이밍은 통째로 버린다`() {
        val subject = cache()
        writeAudio(subject, "key", 5_000)
        subject.timingFile("key").writeText("0,2,100\n망가진 줄\n3,5,400")

        assertTrue(subject.find("key")?.wordTimings?.isEmpty() == true)
    }

    @Test
    fun `찾을 때 접근 시각을 올린다`() {
        val subject = cache()
        writeAudio(subject, "key", 5_000)
        val before = subject.audioFile("key").lastModified()

        clock += day
        subject.find("key")

        assertTrue(
            "시각이 올라가야 한다 (before=$before)",
            subject.audioFile("key").lastModified() > before,
        )
    }

    @Test
    fun `상한 안이면 아무것도 지우지 않는다`() {
        val subject = cache(maxBytes = 100_000)
        writeAudio(subject, "a", 10_000)
        writeAudio(subject, "b", 10_000)

        assertEquals(0, subject.trim())
        assertEquals(20_000, subject.totalBytes())
    }

    @Test
    fun `상한을 넘으면 오래 안 쓴 것부터 지운다`() {
        val subject = cache(maxBytes = 25_000)
        writeAudio(subject, "oldest", 10_000)
        clock += day
        writeAudio(subject, "middle", 10_000)
        clock += day
        writeAudio(subject, "newest", 10_000)

        subject.trim()

        assertFalse("가장 오래된 것이 남았다", subject.audioFile("oldest").isFile)
        assertTrue("최근 것이 지워졌다", subject.audioFile("newest").isFile)
        assertTrue(subject.totalBytes() <= 25_000)
    }

    @Test
    fun `타이밍도 소리와 함께 지운다`() {
        val subject = cache(maxBytes = 5_000)
        writeAudio(subject, "old", 10_000)
        subject.storeTimings("old", listOf(WordTiming(0, 2, 100)))

        subject.trim()

        assertFalse("소리가 남았다", subject.audioFile("old").isFile)
        assertFalse("타이밍만 남았다", subject.timingFile("old").isFile)
    }

    @Test
    fun `비우면 전부 사라진다`() {
        val subject = cache()
        writeAudio(subject, "a", 1_000)
        subject.storeTimings("a", listOf(WordTiming(0, 1, 0)))

        subject.clear()

        assertEquals(0, subject.totalBytes())
        assertEquals(0, folder.root.listFiles()?.size)
    }

    @Test
    fun `키는 파일 이름으로 쓸 수 있는 글자만 쓴다`() {
        val key = cache().keyOf(SynthesisRequest("경로/에 쓸 수 없는 문장? *", "voice", 1f), "engine")

        assertTrue("파일 이름에 못 쓰는 글자가 있다: $key", key.all { it in "0123456789abcdef" })
        assertEquals("sha256 은 64자다", 64, key.length)
        assertFalse(File(folder.root, "$key.wav").name.contains('/'))
    }
}
