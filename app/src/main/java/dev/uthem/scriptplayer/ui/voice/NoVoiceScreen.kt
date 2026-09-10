package dev.uthem.scriptplayer.ui.voice

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.ui.component.AppButton
import dev.uthem.scriptplayer.ui.component.AppOutlinedButton
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 오프라인 한국어 음성이 없을 때.
 *
 * 이 앱은 기기의 음성으로만 소리를 만들므로, 음성이 없으면 아무것도 할 수 없다.
 * 그 사실을 감추지 않고 무엇을 하면 되는지 알려 준다 — 조용히 실패하면 사용자는
 * "재생을 눌렀는데 소리가 안 난다" 만 겪는다.
 *
 * 실측 기기에는 네 종이 있었지만, 음성 데이터를 받지 않은 기기도 있다.
 */
@Composable
fun NoVoiceScreen(
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .padding(Space.x6),
        verticalArrangement = Arrangement.spacedBy(Space.x3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "읽어 줄 목소리가 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            "이 앱은 인터넷을 쓰지 않고 기기에 설치된 음성으로 읽습니다. " +
                "한국어 음성 데이터를 한 번 받아 두면 그다음부터는 비행기 모드에서도 돕니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
        Text(
            "설정 → 언어 및 입력 → 음성 합성 → 한국어",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textTertiary,
        )
        AppButton(
            text = "음성 데이터 받기",
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        )
        // 받고 돌아온 뒤 앱을 다시 켜지 않아도 되게 한다
        AppOutlinedButton(
            text = "다시 확인",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 음성 데이터를 받는 시스템 화면으로 보낸다.
 *
 * 엔진이 이 인텐트를 받지 않는 기기가 있어 실패할 수 있다. 그때는 위에 적어 둔
 * 설정 경로를 손으로 따라가야 하므로, 글로도 함께 알려 준다.
 */
@Composable
fun rememberInstallVoiceAction(): () -> Unit {
    val context = LocalContext.current
    return {
        runCatching {
            context.startActivity(
                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Preview
@Composable
private fun NoVoiceLightPreview() {
    AppTheme(darkTheme = false) { NoVoiceScreen(onOpenSettings = {}, onRetry = {}) }
}

@Preview
@Composable
private fun NoVoiceDarkPreview() {
    AppTheme(darkTheme = true) { NoVoiceScreen(onOpenSettings = {}, onRetry = {}) }
}
