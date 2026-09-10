package dev.uthem.scriptplayer.spike

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.sin

/*
 * 1단계 검증 앱 전용. 실측이 끝나면 spike 패키지째로 지운다.
 */

/**
 * 미디어 버튼을 눌러 볼 소리를 만든다.
 *
 * 2초에 한 번 짧게 울리게 한다. 끊기지 않는 음을 깔면 재생·정지를 눌러도 무엇이 바뀌었는지
 * 귀로 구분하기 어렵고, 20초를 내리 들으면 거슬린다.
 */
fun writeProbeTone(file: File, seconds: Int = 20, sampleRate: Int = 44_100): File {
    val total = seconds * sampleRate
    val samples = ShortArray(total)
    val beep = (sampleRate * 0.15).toInt()
    val period = sampleRate * 2
    for (i in 0 until total) {
        val phase = i % period
        if (phase < beep) {
            // 앞뒤를 부드럽게 깎는다 — 각을 세우면 딱 하는 잡음이 섞인다
            val envelope = sin(PI * phase / beep)
            samples[i] = (sin(2 * PI * 440 * i / sampleRate) * envelope * 6000).toInt().toShort()
        }
    }
    writeWav(file, samples, sampleRate)
    return file
}

private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
    val dataSize = samples.size * 2
    DataOutputStream(FileOutputStream(file).buffered()).use { out ->
        fun ascii(text: String) = out.write(text.toByteArray(Charsets.US_ASCII))
        fun le16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
        fun le32(v: Int) = out.write(
            byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()),
        )

        ascii("RIFF"); le32(36 + dataSize); ascii("WAVE")
        ascii("fmt "); le32(16)
        le16(1); le16(1)                    // PCM, 모노
        le32(sampleRate); le32(sampleRate * 2)
        le16(2); le16(16)
        ascii("data"); le32(dataSize)
        samples.forEach { le16(it.toInt()) }
    }
}

/**
 * WAV 의 샘플레이트를 헤더에서 읽는다.
 *
 * `onRangeStart` 가 주는 frame 을 시각으로 바꾸려면 이 값이 필요하다(atMs = frame * 1000 / rate).
 * 엔진이 말해 주는 값을 믿지 않고 실제 파일에서 읽는다 — 음성마다 다를 수 있다.
 *
 * fmt 청크의 위치를 고정하지 않고 청크를 훑는다. 오프셋 24 로 못 박아도 대개 맞지만,
 * 엔진이 청크를 더 끼워 넣으면 엉뚱한 4바이트를 샘플레이트로 읽는다.
 */
fun readWavSampleRate(file: File): Int? = runCatching {
    RandomAccessFile(file, "r").use { raf ->
        val riff = ByteArray(4).also { raf.readFully(it) }.decodeToString()
        raf.skipBytes(4)
        val wave = ByteArray(4).also { raf.readFully(it) }.decodeToString()
        if (riff != "RIFF" || wave != "WAVE") return@use null

        while (raf.filePointer < raf.length() - 8) {
            val id = ByteArray(4).also { raf.readFully(it) }.decodeToString()
            val size = readLe32(raf)
            if (id == "fmt ") {
                raf.skipBytes(4) // 포맷 코드(2) + 채널 수(2)
                return@use readLe32(raf)
            }
            // 청크는 짝수 바이트로 정렬된다
            raf.skipBytes(size + (size and 1))
        }
        null
    }
}.getOrNull()

private fun readLe32(raf: RandomAccessFile): Int {
    val b = ByteArray(4).also { raf.readFully(it) }
    return (b[0].toInt() and 0xff) or
        ((b[1].toInt() and 0xff) shl 8) or
        ((b[2].toInt() and 0xff) shl 16) or
        ((b[3].toInt() and 0xff) shl 24)
}
