package dev.uthem.scriptplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.data.ThemeChoice
import dev.uthem.scriptplayer.tts.VoiceInfo
import dev.uthem.scriptplayer.ui.component.AppCard
import dev.uthem.scriptplayer.ui.component.AppChip
import dev.uthem.scriptplayer.ui.component.AppOutlinedButton
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

/**
 * 설정. 상태를 받기만 한다.
 *
 * 화자 배정을 **대본마다가 아니라 자리별로** 둔다. 화자 id 가 등장 순서라 어느 대본이든
 * "첫 화자는 이 목소리" 가 성립하고, 한 번 정하면 끝이다.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onTheme: (ThemeChoice) -> Unit,
    onSpeakerVoice: (slot: Int, voiceName: String?) -> Unit,
    onClearCache: () -> Unit,
    onInstallVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Space.x4),
        verticalArrangement = Arrangement.spacedBy(Space.x5),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            AppOutlinedButton(text = "←", onClick = onBack)
            Text(
                "설정",
                style = MaterialTheme.typography.headlineSmall,
                color = AppTheme.colors.textPrimary,
            )
        }

        Section(
            title = "화자 목소리",
            note = "대본에 화자가 여럿이면 나온 순서대로 이 목소리를 씁니다. " +
                "«자동» 으로 두면 남은 목소리 중에서 알아서 고릅니다.",
        ) {
            if (state.voices.isEmpty()) {
                MissingVoices(onInstallVoice)
            } else {
                state.speakerSlots.forEachIndexed { slot, chosen ->
                    SpeakerSlot(
                        slot = slot,
                        chosen = chosen,
                        voices = state.voices,
                        onPick = { onSpeakerVoice(slot, it) },
                    )
                }
            }
        }

        Section(title = "테마") {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                ThemeChoice.entries.forEach { choice ->
                    AppChip(
                        label = choice.label(),
                        selected = state.theme == choice,
                        onClick = { onTheme(choice) },
                    )
                }
            }
        }

        Section(
            title = "만들어 둔 소리",
            note = "한 번 만든 소리는 남겨 두어 두 번째부터는 기다리지 않습니다. " +
                "지워도 다시 만들면 되니 공간이 필요하면 비우세요.",
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.cacheLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                )
                AppOutlinedButton(
                    text = "비우기",
                    onClick = onClearCache,
                    enabled = state.cacheBytes > 0,
                )
            }
        }

        Section(title = "이 앱에 대해") {
            Text(
                "인터넷을 쓰지 않습니다. 네트워크 권한을 아예 선언하지 않았고, " +
                    "기기에 설치된 음성으로만 소리를 만듭니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    note: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        }
        content()
    }
}

/**
 * 화자 한 자리.
 *
 * 목소리 목록을 펼쳐 두지 않고 접는다. 처음에 자리마다 칩 다섯 개를 늘어놓았더니
 * 네 자리에 스무 줄이 되어, 테마와 캐시 설정이 한참 아래로 밀렸다 — 설정 화면에서
 * 목소리 고르기가 전부를 차지할 이유가 없다.
 */
@Composable
private fun SpeakerSlot(
    slot: Int,
    chosen: String?,
    voices: List<VoiceInfo>,
    onPick: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val label = chosen?.let { name -> voices.firstOrNull { it.name == name }?.shortName() } ?: "자동"

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${slot + 1}번째 화자",
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
            )
            Box {
                AppChip(label = "$label ▾", selected = chosen != null, onClick = { open = true })
                DropdownMenu(
                    expanded = open,
                    onDismissRequest = { open = false },
                    containerColor = AppTheme.colors.surface,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "자동으로 고르기",
                                color = AppTheme.colors.textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            open = false
                            onPick(null)
                        },
                    )
                    voices.forEach { voice ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        voice.shortName(),
                                        color = AppTheme.colors.textPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    // 전체 이름도 함께 낸다 — 짧은 조각만으로는 어느 것인지 헷갈린다
                                    Text(
                                        voice.name,
                                        color = AppTheme.colors.textTertiary,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            onClick = {
                                open = false
                                onPick(voice.name)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingVoices(onInstallVoice: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
        Text(
            "쓸 수 있는 한국어 음성이 없습니다. 한 번 받아 두면 그다음부터는 " +
                "비행기 모드에서도 돕니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
        AppOutlinedButton(
            text = "음성 데이터 받기",
            onClick = onInstallVoice,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.SYSTEM -> "시스템"
    ThemeChoice.LIGHT -> "밝게"
    ThemeChoice.DARK -> "어둡게"
}

/**
 * 음성 이름에서 알아볼 만한 조각만 뽑는다.
 *
 * `ko-kr-x-ism-local` 에서 `ism` 이 목소리를 가르는 부분이다. 전체 이름을 칩에 넣으면
 * 칩이 화면을 넘어가고, 넷을 나란히 두면 무엇이 다른지도 안 보인다.
 */
internal fun VoiceInfo.shortName(): String {
    val parts = name.split('-')
    val marker = parts.getOrNull(parts.size - 2)
    return marker?.takeIf { it.isNotEmpty() && it != "language" } ?: name
}

@Preview
@Composable
private fun SettingsLightPreview() {
    AppTheme(darkTheme = false) {
        SettingsScreen(
            state = previewSettings(),
            onBack = {},
            onTheme = {},
            onSpeakerVoice = { _, _ -> },
            onClearCache = {},
            onInstallVoice = {},
        )
    }
}
