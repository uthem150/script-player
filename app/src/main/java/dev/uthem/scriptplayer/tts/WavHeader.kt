package dev.uthem.scriptplayer.tts

import java.io.File
import java.io.RandomAccessFile

/**
 * WAV 의 샘플레이트를 헤더에서 읽는다.
 *
 * `onRangeStart` 가 주는 프레임 위치를 시각으로 바꾸려면 이 값이 필요하다
 * (`atMs = frame * 1000 / sampleRate`). 엔진이 말해 주는 값을 믿지 않고 실제 파일에서
 * 읽는다 — 음성마다 다를 수 있고, 실측에서 Google TTS 한국어는 24000Hz 였다.
 *
 * **오프셋 24 로 못 박지 않고 청크를 훑는다.** 대개는 맞지만, 엔진이 청크를 더 끼워 넣으면
 * 엉뚱한 4바이트를 샘플레이트로 읽어 단어 타이밍이 전부 어긋난다.
 */
fun readWavSampleRate(file: File): Int? = runCatching {
    RandomAccessFile(file, "r").use { raf ->
        if (raf.length() < MIN_HEADER_BYTES) return@use null

        val riff = raf.readAscii(4)
        raf.skipBytes(4) // 전체 크기
        val wave = raf.readAscii(4)
        if (riff != "RIFF" || wave != "WAVE") return@use null

        while (raf.filePointer <= raf.length() - CHUNK_HEADER_BYTES) {
            val id = raf.readAscii(4)
            val size = raf.readLe32()
            if (id == "fmt ") {
                raf.skipBytes(4) // 포맷 코드(2) + 채널 수(2)
                return@use raf.readLe32().takeIf { it > 0 }
            }
            if (size < 0) return@use null
            // 청크는 짝수 바이트로 정렬된다
            raf.skipBytes(size + (size and 1))
        }
        null
    }
}.getOrNull()

private fun RandomAccessFile.readAscii(length: Int): String =
    ByteArray(length).also { readFully(it) }.decodeToString()

private fun RandomAccessFile.readLe32(): Int {
    val bytes = ByteArray(4).also { readFully(it) }
    return (bytes[0].toInt() and 0xff) or
        ((bytes[1].toInt() and 0xff) shl 8) or
        ((bytes[2].toInt() and 0xff) shl 16) or
        ((bytes[3].toInt() and 0xff) shl 24)
}

/**
 * WAV 의 재생 길이.
 *
 * 파일 크기와 샘플레이트로 낸다. ExoPlayer 가 항목을 준비한 뒤에 알려주는 값을 기다리면
 * 진도 막대가 한동안 어림값으로 남는데, 파일은 이미 손에 있으므로 바로 잴 수 있다.
 *
 * 16비트 모노를 전제한다 — `synthesizeToFile` 이 그 형식으로 낸다(실측 확인).
 */
fun wavDurationMs(file: File): Long? {
    val sampleRate = readWavSampleRate(file) ?: return null
    val dataBytes = file.length() - HEADER_BYTES
    if (dataBytes <= 0) return null
    return dataBytes * 1000L / (sampleRate.toLong() * BYTES_PER_SAMPLE)
}

private const val MIN_HEADER_BYTES = 12L
private const val CHUNK_HEADER_BYTES = 8L
private const val HEADER_BYTES = 44L
private const val BYTES_PER_SAMPLE = 2L
