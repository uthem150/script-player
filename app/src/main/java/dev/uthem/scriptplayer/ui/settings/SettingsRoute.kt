package dev.uthem.scriptplayer.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.uthem.scriptplayer.appContainer
import dev.uthem.scriptplayer.data.AppSettings
import dev.uthem.scriptplayer.tts.AndroidSynthesizer
import dev.uthem.scriptplayer.tts.offlineKorean
import dev.uthem.scriptplayer.ui.voice.rememberInstallVoiceAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 설정에 상태를 붙이는 층.
 *
 * 음성 목록을 얻으려면 TTS 엔진을 열어야 한다. 화면을 열 때 한 번 열고 바로 닫는다 —
 * 설정을 보는 동안 엔진을 붙잡고 있으면 재생 쪽과 다툰다.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val settings = container.settings
    val scope = rememberCoroutineScope()
    val installVoice = rememberInstallVoiceAction()

    val theme by settings.theme.collectAsStateWithLifecycle()
    val slots by settings.speakerVoices.collectAsStateWithLifecycle()
    var voices by remember { mutableStateOf(emptyList<dev.uthem.scriptplayer.tts.VoiceInfo>()) }
    var cacheBytes by remember { mutableStateOf(0L) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        val synthesizer = AndroidSynthesizer(context, container.audioCache)
        voices = if (synthesizer.open().isSuccess) {
            synthesizer.availableVoices().offlineKorean()
        } else {
            emptyList()
        }
        synthesizer.close()
        // 파일을 훑는 일이라 본 흐름에서 비껴 둔다
        cacheBytes = withContext(Dispatchers.IO) { container.audioCache.totalBytes() }
    }

    SettingsScreen(
        state = SettingsUiState(
            theme = theme,
            voices = voices,
            speakerSlots = List(AppSettings.MAX_SPEAKER_SLOTS) { slot ->
                slots.getOrNull(slot)?.takeIf { it.isNotEmpty() }
            },
            cacheBytes = cacheBytes,
        ),
        onBack = onBack,
        onTheme = settings::setTheme,
        onSpeakerVoice = settings::setSpeakerVoice,
        onClearCache = {
            scope.launch {
                withContext(Dispatchers.IO) { container.audioCache.clear() }
                cacheBytes = 0
            }
        },
        onInstallVoice = {
            installVoice()
            // 받고 돌아오면 목록을 다시 읽는다
            reload++
        },
        modifier = modifier,
    )
}
