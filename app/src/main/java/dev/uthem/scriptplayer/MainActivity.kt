package dev.uthem.scriptplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.uthem.scriptplayer.data.ThemeChoice
import dev.uthem.scriptplayer.ui.library.AddScriptRoute
import dev.uthem.scriptplayer.ui.library.LibraryRoute
import dev.uthem.scriptplayer.ui.player.PlayerRoute
import dev.uthem.scriptplayer.ui.settings.SettingsRoute
import dev.uthem.scriptplayer.ui.share.ShareConfirmRoute
import dev.uthem.scriptplayer.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    /**
     * 공유로 들어온 글.
     *
     * 액티비티가 들고 있는다 — 인텐트로 오는 것이라 컴포즈 상태로 시작할 수 없고,
     * 앱이 이미 떠 있는 채로 또 공유되면 [onNewIntent] 로 들어온다.
     */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 바 아이콘 색을 테마에 맞춰 준다. 이걸 빼면 밝은 배경에 흰 아이콘이 얹혀 안 보인다.
        enableEdgeToEdge()
        sharedText = intent?.plainTextToRead()

        setContent {
            val settings = appContainer.settings
            val choice by settings.theme.collectAsStateWithLifecycle()
            /*
             * 사용자가 고른 테마가 시스템보다 먼저다.
             *
             * «시스템» 으로 두면 OS 를 따르고, 밝게·어둡게를 고르면 그것을 지킨다 —
             * 폰 전체를 어둡게 쓰면서 이 앱만 밝게 보고 싶을 때가 있다.
             */
            AppTheme(
                darkTheme = when (choice) {
                    ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                    ThemeChoice.LIGHT -> false
                    ThemeChoice.DARK -> true
                },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colors.background,
                ) {
                    AppRoot(
                        sharedText = sharedText,
                        onSharedHandled = { sharedText = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // setIntent 를 함께 해 둔다 — 안 하면 이 액티비티가 계속 처음 인텐트를 가리킨다
        setIntent(intent)
        sharedText = intent.plainTextToRead()
    }
}

/**
 * 공유로 온 텍스트를 꺼낸다.
 *
 * 빈 글은 없는 것으로 본다. 메신저에서 아무것도 고르지 않고 공유하면 빈 문자열이 오는데,
 * 그때 확인 화면을 띄우면 담을 것도 없는 화면이 뜬다.
 */
private fun Intent.plainTextToRead(): String? {
    if (action != Intent.ACTION_SEND || type != "text/plain") return null
    return getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
}

/**
 * 화면 사이 이동.
 *
 * Navigation 을 얹지 않고 "지금 어느 화면인가" 를 값 하나로 둔다. 화면이 넷이고 흐름이
 * 한 줄기(보관함 → 재생기 / 보관함 → 새 대본)라, 경로 표를 만들면 코드보다 설정이 많아진다.
 * 설정 화면이 붙고 흐름이 갈라지면 그때 바꾼다.
 *
 * 재생 중인 대본 id 를 [rememberSaveable] 로 들고 있어, 화면을 돌려도 재생기가 닫히지 않는다.
 */
@Composable
private fun AppRoot(
    sharedText: String?,
    onSharedHandled: () -> Unit,
) {
    var addingScript by rememberSaveable { mutableStateOf(false) }
    var showingSettings by rememberSaveable { mutableStateOf(false) }
    var playingScriptId by rememberSaveable { mutableStateOf<String?>(null) }

    val openScript = playingScriptId
    when {
        // 공유가 가장 앞선다. 공유로 앱이 열린 것이라 다른 화면을 먼저 보여줄 이유가 없다
        sharedText != null -> {
            BackHandler(onBack = onSharedHandled)
            ShareConfirmRoute(raw = sharedText, onDone = onSharedHandled)
        }

        openScript != null -> {
            BackHandler { playingScriptId = null }
            PlayerRoute(scriptId = openScript, onBack = { playingScriptId = null })
        }

        showingSettings -> {
            BackHandler { showingSettings = false }
            SettingsRoute(onBack = { showingSettings = false })
        }

        addingScript -> {
            // 뒤로 가기로 붙여넣기 화면을 닫는다. 없으면 앱이 통째로 닫힌다
            BackHandler { addingScript = false }
            AddScriptRoute(onDone = { addingScript = false })
        }

        else -> LibraryRoute(
            onOpenAdd = { addingScript = true },
            onOpenScript = { playingScriptId = it },
            onOpenSettings = { showingSettings = true },
        )
    }
}
