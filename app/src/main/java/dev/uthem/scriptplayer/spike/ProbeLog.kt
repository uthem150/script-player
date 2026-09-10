package dev.uthem.scriptplayer.spike

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * 1단계 검증 앱 전용. 실측이 끝나면 spike 패키지째로 지운다.
 *
 * 서비스에서 일어난 일을 화면으로 올리는 통로. adb 로 로그를 읽을 수 없는 상황을
 * 전제로 만든 앱이라, 이벤트를 눈에 보이는 곳에 쌓아야 한다.
 */
object ProbeLog {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines = _lines.asStateFlow()

    fun record(message: String) {
        val line = "${LocalTime.now().format(formatter)}  $message"
        // 오래된 것부터 버린다 — 화면에 쌓기만 하면 눌러 본 직후의 것을 찾기 어렵다
        _lines.value = (_lines.value + line).takeLast(40)
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
