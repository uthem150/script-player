package dev.uthem.scriptplayer.ui.settings

import dev.uthem.scriptplayer.data.AppSettings
import dev.uthem.scriptplayer.data.ThemeChoice
import dev.uthem.scriptplayer.tts.VoiceInfo

data class SettingsUiState(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    /** 쓸 수 있는 한국어 음성. 비어 있으면 음성을 받아야 한다 */
    val voices: List<VoiceInfo> = emptyList(),
    /** 자리별로 고른 음성 이름. null 이면 자동 */
    val speakerSlots: List<String?> = List(AppSettings.MAX_SPEAKER_SLOTS) { null },
    val cacheBytes: Long = 0,
) {
    val cacheLabel: String get() = cacheBytes.asStorageLabel()
}

/**
 * 저장 공간을 읽기 좋게 적는다.
 *
 * 바이트 수를 그대로 보여주면 큰지 작은지 알 수 없다. 실측 기준 10분 대본이 약 27MB 라
 * MB 단위가 이 앱에 맞는 눈금이다.
 */
internal fun Long.asStorageLabel(): String = when {
    this <= 0 -> "만들어 둔 소리가 없습니다"
    this < 1024 * 1024 -> "1MB 미만"
    else -> "약 ${this / (1024 * 1024)}MB"
}

internal fun previewSettings(): SettingsUiState = SettingsUiState(
    theme = ThemeChoice.SYSTEM,
    voices = listOf(
        VoiceInfo("ko-kr-x-ism-local", "ko", 400, needsNetwork = false, notInstalled = false),
        VoiceInfo("ko-kr-x-kob-local", "ko", 400, needsNetwork = false, notInstalled = false),
        VoiceInfo("ko-kr-x-koc-local", "ko", 400, needsNetwork = false, notInstalled = false),
        VoiceInfo("ko-kr-x-kod-local", "ko", 400, needsNetwork = false, notInstalled = false),
    ),
    speakerSlots = listOf("ko-kr-x-ism-local", "ko-kr-x-kob-local", null, null),
    cacheBytes = 84L * 1024 * 1024,
)

internal fun previewSettingsWithoutVoices(): SettingsUiState = SettingsUiState(cacheBytes = 0)
