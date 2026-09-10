@file:OptIn(UnstableApi::class)

package dev.uthem.scriptplayer.spike

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/*
 * 1단계 검증 앱. 실측이 끝나면 spike 패키지째로 지운다.
 *
 * adb 를 쓸 수 없는 상황을 전제로 만들었다. 그래서 결과를 로그로 뱉지 않고 화면에 띄우고,
 * 복사 버튼을 붙인다. 릴리즈로 받아 설치하고 화면 한 번 보고 복사해 보내면 그게 측정 결과다.
 */

@Composable
fun SpikeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val probe = remember { TtsProbe(context) }
    var status by remember { mutableStateOf("엔진을 여는 중…") }
    var voices by remember { mutableStateOf<List<VoiceRow>>(emptyList()) }
    var measurement by remember { mutableStateOf<Measurement?>(null) }
    var busy by remember { mutableStateOf(false) }

    val logLines by ProbeLog.lines.collectAsStateWithLifecycle()
    val controller = rememberProbeController()

    // 알림 권한이 없으면 포그라운드 서비스 알림이 보이지 않아, 잠금화면 컨트롤을 확인할 수 없다.
    val notifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        val opened = probe.open()
        status = if (opened.isSuccess) {
            voices = probe.voices()
            val korean = probe.koreanOfflineVoices()
            if (korean.isEmpty()) {
                "엔진은 열렸지만 오프라인 한국어 음성이 없습니다"
            } else {
                "준비됨 — 한국어 오프라인 음성 ${korean.size}개"
            }
        } else {
            opened.exceptionOrNull()?.message ?: "엔진을 열지 못했습니다"
        }
    }

    DisposableEffect(Unit) {
        onDispose { probe.close() }
    }

    val report = buildReport(status, voices, measurement, logLines)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("1단계 실측", style = MaterialTheme.typography.headlineSmall)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = {
                busy = true
                scope.launch {
                    measurement = withContext(Dispatchers.Default) { measure(context, probe) }
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "합성 중… (한동안 걸립니다)" else "① 합성 속도·단어 타이밍 재기")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val active = controller ?: return@Button
                    val tone = File(context.cacheDir, "probe-tone.wav")
                    if (!tone.exists()) writeProbeTone(tone)
                    active.setMediaItem(MediaItem.fromUri(Uri.fromFile(tone)))
                    active.prepare()
                    active.play()
                },
                enabled = controller != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("② 소리 켜기")
            }
            OutlinedButton(
                onClick = { controller?.pause() },
                enabled = controller != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("정지")
            }
        }

        Text(
            "소리를 켠 뒤 에어팟을 한 번·두 번 탭해 보세요. 아래 기록에 재생·정지가 찍히면 " +
                "버튼이 앱까지 들어온 것입니다. 화면을 끄고 한참 뒤에 다시 봐도 소리가 " +
                "계속 나는지도 확인해 주세요.",
            style = MaterialTheme.typography.bodySmall,
        )

        SectionCard(title = "미디어 버튼 기록") {
            if (logLines.isEmpty()) {
                Text("아직 없음", style = MaterialTheme.typography.bodySmall)
            } else {
                logLines.asReversed().forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }

        SectionCard(title = "결과") {
            Text(report, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }

        Button(
            onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("1단계 실측", report))
                ProbeLog.record("결과를 복사했습니다")
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("③ 결과 복사")
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun rememberProbeController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackProbeService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            controller?.release()
            controller = null
        }
    }
    return controller
}

private data class Measurement(
    val engine: String?,
    val voiceUsed: String?,
    val sentences: List<SentenceProbe>,
)

private suspend fun measure(
    context: android.content.Context,
    probe: TtsProbe,
): Measurement {
    val raw = context.assets.open("sample-script.md").bufferedReader().use { it.readText() }
    val sentences = crudeSentences(raw)
    val voice = probe.koreanOfflineVoices().firstOrNull()
    val dir = File(context.cacheDir, "probe").apply { mkdirs() }

    val results = sentences.mapIndexed { index, text ->
        probe.synthesize(text, voice, File(dir, "s$index.wav"))
    }
    return Measurement(probe.defaultEngineName, voice?.name, results)
}

/** WAV 바이트에서 실제 오디오 길이를 낸다. 헤더 44바이트를 빼고 16비트 모노로 계산한다. */
private fun SentenceProbe.audioMs(): Long {
    val rate = sampleRate ?: return 0
    if (audioBytes <= 44) return 0
    return (audioBytes - 44) * 1000 / (rate.toLong() * 2)
}

private fun buildReport(
    status: String,
    voices: List<VoiceRow>,
    measurement: Measurement?,
    logLines: List<String>,
): String = buildString {
    appendLine("== 대본 플레이어 · 1단계 실측 ==")
    appendLine(
        "기기: ${Build.MANUFACTURER} ${Build.MODEL} / " +
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
    )
    appendLine("상태: $status")
    appendLine()

    val koreanOffline = voices.count { it.locale.startsWith("ko") && it.usableOffline }
    appendLine("[음성] 전체 ${voices.size}개 · 한국어 오프라인 ${koreanOffline}개")
    voices.filter { it.locale.startsWith("ko") }.forEach { row ->
        appendLine(
            "  ${row.locale}  ${row.name}  " +
                "오프라인=${if (row.usableOffline) "O" else "X"}  품질=${row.quality}",
        )
    }
    if (voices.none { it.locale.startsWith("ko") }) {
        appendLine("  한국어 음성이 하나도 없음")
    }
    appendLine()

    if (measurement == null) {
        appendLine("[합성] 아직 재지 않음 — ① 을 눌러 주세요")
    } else {
        val done = measurement.sentences
        val failures = done.count { it.error != null }
        val chars = done.sumOf { it.chars }
        val elapsed = done.sumOf { it.elapsedMs }
        val audio = done.sumOf { it.audioMs() }
        val ranges = done.sumOf { it.rangeCount }

        appendLine("[합성]  엔진=${measurement.engine}  음성=${measurement.voiceUsed ?: "기본"}")
        appendLine("  문장 ${done.size}개 · 글자 ${chars}자 · 실패 ${failures}건")
        appendLine("  합성 ${elapsed / 1000.0}초 · 오디오 ${audio / 1000}초")
        if (elapsed > 0 && audio > 0) {
            val factor = audio.toDouble() / elapsed
            appendLine("  실시간 대비 ${"%.1f".format(factor)}배")
            appendLine("  → 10분 대본이면 약 ${"%.0f".format(600 / factor)}초에 준비됨")
        }
        appendLine("  샘플레이트=${done.firstOrNull { it.sampleRate != null }?.sampleRate ?: "알 수 없음"}")
        appendLine()
        appendLine("[단어 타이밍 onRangeStart]")
        if (ranges > 0) {
            appendLine("  수신 ${ranges}건 → 단어 탭 재생을 정확값으로 할 수 있음")
        } else {
            appendLine("  미수신 → 글자 비율 추정으로 대체, 단어 하이라이트는 포기")
        }
        done.filter { it.error != null }.take(5).forEach {
            appendLine("  실패: ${it.error}")
        }
    }
    appendLine()

    appendLine("[미디어 버튼]")
    if (logLines.isEmpty()) {
        appendLine("  기록 없음 — ② 를 누르고 에어팟을 탭해 주세요")
    } else {
        logLines.forEach { appendLine("  $it") }
    }
}
