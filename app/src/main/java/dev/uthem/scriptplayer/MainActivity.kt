package dev.uthem.scriptplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.uthem.scriptplayer.ui.library.AddScriptRoute
import dev.uthem.scriptplayer.ui.library.LibraryRoute
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
            AppTheme {
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
 * 화면이 적어 참·거짓 하나로 둔다. Navigation 을 얹으면 지금은 설정이 코드보다 많아진다.
 * 재생기와 설정이 들어오는 7·8단계에 제대로 바꾼다.
 */
@Composable
private fun AppRoot(
    sharedText: String?,
    onSharedHandled: () -> Unit,
) {
    var addingScript by rememberSaveable { mutableStateOf(false) }

    when {
        // 공유가 가장 앞선다. 공유로 앱이 열린 것이라 다른 화면을 먼저 보여줄 이유가 없다
        sharedText != null -> {
            BackHandler(onBack = onSharedHandled)
            ShareConfirmRoute(raw = sharedText, onDone = onSharedHandled)
        }
        addingScript -> {
            // 뒤로 가기로 붙여넣기 화면을 닫는다. 없으면 앱이 통째로 닫힌다
            BackHandler { addingScript = false }
            AddScriptRoute(onDone = { addingScript = false })
        }
        else -> LibraryRoute(onOpenAdd = { addingScript = true })
    }
}
