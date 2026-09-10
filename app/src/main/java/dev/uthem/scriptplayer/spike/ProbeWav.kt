package dev.uthem.scriptplayer.spike

import java.io.File
import java.io.RandomAccessFile

/**
 * WAV 의 샘플레이트를 헤더에서 읽는다.
 *
 * `onRangeStart` 가 주는 frame 을 시각으로 바꾸려면 이 값이 필요하다(atMs = frame * 1000 / rate).
 * 엔진이 말해 주는 값을 믿지 않고 실제 파일에서 읽는다 — 음성마다 다를 수 있다.
 * (실측: Google TTS 한국어는 24000Hz)
 *
 * fmt 청크의 위치를 고정하지 않고 청크를 훑는다. 오프셋 24 로 못 박아도 대개 맞지만,
 * 엔진이 청크를 더 끼워 넣으면 엉뚱한 4바이트를 샘플레이트로 읽는다.
 *
 * 검증 앱에서 시작했지만 5단계 합성 파이프라인에서도 쓴다 — 그때 spike 밖으로 옮긴다.
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
