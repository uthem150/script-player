package dev.uthem.scriptplayer.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * WAV 헤더 읽기.
 *
 * 단어 타이밍이 전부 이 숫자에 달렸다 — 프레임을 시각으로 바꾸는 나눗셈의 분모다.
 * 잘못 읽으면 단어를 탭했을 때 엉뚱한 자리에서 재생된다.
 */
class WavHeaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun wav(
        sampleRate: Int = 24_000,
        extraChunkBeforeFmt: Boolean = false,
        riff: String = "RIFF",
        wave: String = "WAVE",
    ): File {
        val body = ByteArrayOutputStream()
        fun ascii(text: String) = body.write(text.toByteArray(Charsets.US_ASCII))
        fun le16(value: Int) = body.write(byteArrayOf(value.toByte(), (value shr 8).toByte()))
        fun le32(value: Int) = body.write(
            byteArrayOf(
                value.toByte(),
                (value shr 8).toByte(),
                (value shr 16).toByte(),
                (value shr 24).toByte(),
            ),
        )

        ascii(riff); le32(0); ascii(wave)
        if (extraChunkBeforeFmt) {
            // 엔진이 끼워 넣는 잡다한 청크. 오프셋을 못 박으면 여기서 어긋난다
            ascii("LIST"); le32(4); body.write(ByteArray(4))
        }
        ascii("fmt "); le32(16)
        le16(1); le16(1)
        le32(sampleRate); le32(sampleRate * 2)
        le16(2); le16(16)
        ascii("data"); le32(8); body.write(ByteArray(8))

        return File(folder.root, "sample-${System.nanoTime()}.wav")
            .also { it.writeBytes(body.toByteArray()) }
    }

    @Test
    fun `샘플레이트를 읽는다`() {
        assertEquals(24_000, readWavSampleRate(wav(sampleRate = 24_000)))
        assertEquals(44_100, readWavSampleRate(wav(sampleRate = 44_100)))
    }

    /** 이것이 오프셋을 못 박지 않는 이유다. */
    @Test
    fun `fmt 앞에 다른 청크가 있어도 읽는다`() {
        assertEquals(24_000, readWavSampleRate(wav(extraChunkBeforeFmt = true)))
    }

    @Test
    fun `WAV 가 아니면 읽지 않는다`() {
        assertNull(readWavSampleRate(wav(riff = "XXXX")))
        assertNull(readWavSampleRate(wav(wave = "YYYY")))
    }

    @Test
    fun `없는 파일이면 null 이다`() {
        assertNull(readWavSampleRate(File(folder.root, "없음.wav")))
    }

    @Test
    fun `너무 짧은 파일이면 null 이다`() {
        val tiny = File(folder.root, "tiny.wav").also { it.writeBytes(ByteArray(6)) }

        assertNull(readWavSampleRate(tiny))
    }

    /** 합성이 중간에 끊기면 헤더만 남거나 fmt 가 없는 파일이 생긴다. */
    @Test
    fun `fmt 청크가 없으면 null 이다`() {
        val body = ByteArrayOutputStream()
        body.write("RIFF".toByteArray()); body.write(ByteArray(4)); body.write("WAVE".toByteArray())
        val broken = File(folder.root, "broken.wav").also { it.writeBytes(body.toByteArray()) }

        assertNull(readWavSampleRate(broken))
    }

    @Test
    fun `샘플레이트가 0 이면 null 이다`() {
        // 0 을 그대로 쓰면 시각 환산에서 0 으로 나누게 된다
        assertNull(readWavSampleRate(wav(sampleRate = 0)))
    }

    @Test
    fun `프레임을 시각으로 바꾼다`() {
        val audio = wav(sampleRate = 24_000)
        val collected = listOf((0..2) to 0, (3..5) to 12_000, (6..8) to 24_000)

        val timings = collected.toTimings(audio)

        assertEquals(
            listOf(
                WordTiming(0, 3, 0),
                WordTiming(3, 6, 500),
                WordTiming(6, 9, 1_000),
            ),
            timings,
        )
    }

    @Test
    fun `샘플레이트를 못 읽으면 타이밍을 버린다`() {
        // 틀린 시각으로 단어를 짚는 것보다 없는 것이 낫다
        val notWav = File(folder.root, "not-wav").also { it.writeBytes(ByteArray(100)) }

        assertEquals(emptyList<WordTiming>(), listOf((0..2) to 0).toTimings(notWav))
    }

    @Test
    fun `모은 것이 없으면 빈 목록이다`() {
        assertEquals(emptyList<WordTiming>(), emptyList<Pair<IntRange, Int>>().toTimings(wav()))
    }
}
