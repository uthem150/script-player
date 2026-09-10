package dev.uthem.scriptplayer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/**
 * 사용자가 정한 것들.
 *
 * **화자별 음성을 대본마다 저장하지 않고 전역으로 둔다.** 화자 id 가 등장 순서(`s0`·`s1`…)라
 * 어느 대본이든 "첫 화자는 이 목소리" 가 성립한다. 대본별로 두면 Room 이주가 필요하고,
 * 사용자는 대본마다 다시 정해야 한다.
 *
 * [SharedPreferences] 를 쓴다. 담을 것이 값 몇 개라 DataStore 를 들이면 설정이 코드보다
 * 많아진다. 늘어나면 그때 바꾼다.
 */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()

    private val _speakerVoices = MutableStateFlow(readSpeakerVoices())

    /** 화자 자리별 음성 이름. 자리를 비워 두면 자동으로 고른다. */
    val speakerVoices: StateFlow<List<String>> = _speakerVoices.asStateFlow()

    private val _speed = MutableStateFlow(readSpeed())

    /** 마지막으로 쓴 배속. 늘 1.5배로 듣는 사람이 매번 다시 누르지 않게. */
    val speed: StateFlow<Float> = _speed.asStateFlow()

    fun setTheme(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME, choice.name).apply()
        _theme.value = choice
    }

    fun setSpeakerVoice(slot: Int, voiceName: String?) {
        if (slot < 0 || slot >= MAX_SPEAKER_SLOTS) return
        val updated = MutableList(MAX_SPEAKER_SLOTS) { _speakerVoices.value.getOrElse(it) { "" } }
        updated[slot] = voiceName.orEmpty()
        val trimmed = updated.dropLastWhile { it.isEmpty() }
        prefs.edit().putString(KEY_SPEAKER_VOICES, encodeSlots(trimmed)).apply()
        _speakerVoices.value = trimmed
    }

    fun setSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply()
        _speed.value = speed
    }

    private fun readTheme(): ThemeChoice =
        runCatching { ThemeChoice.valueOf(prefs.getString(KEY_THEME, null) ?: "") }
            .getOrDefault(ThemeChoice.SYSTEM)

    private fun readSpeakerVoices(): List<String> =
        decodeSlots(prefs.getString(KEY_SPEAKER_VOICES, null))

    private fun readSpeed(): Float =
        prefs.getFloat(KEY_SPEED, 1.0f).coerceIn(MIN_SPEED, MAX_SPEED)

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_SPEAKER_VOICES = "speakerVoices"
        private const val KEY_SPEED = "speed"

        /**
         * 화자 자리 수.
         *
         * 넷으로 둔다 — 실측 기기의 오프라인 한국어 음성이 네 종이고, 대담에 다섯 사람이
         * 나오는 대본은 드물다. 넘치면 자동 배정이 음성을 돌려 쓰며 음높이로 갈라 준다.
         */
        const val MAX_SPEAKER_SLOTS = 4

        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 3.0f
    }
}

/*
 * 자리별 음성을 한 줄로 담는다.
 *
 * JSON 파서를 끌어올 이유가 없다 — 값이 문자열 몇 개다. 구분자가 음성 이름에 들어갈 수
 * 없는 글자여야 하는데, 실측한 이름은 `ko-kr-x-ism-local` 꼴이라 세로줄을 쓴다.
 */
private const val SLOT_SEPARATOR = '|'

internal fun encodeSlots(voices: List<String>): String =
    voices.joinToString(SLOT_SEPARATOR.toString()) { it.replace(SLOT_SEPARATOR.toString(), "") }

internal fun decodeSlots(encoded: String?): List<String> {
    if (encoded.isNullOrEmpty()) return emptyList()
    return encoded.split(SLOT_SEPARATOR).take(AppSettings.MAX_SPEAKER_SLOTS)
}
