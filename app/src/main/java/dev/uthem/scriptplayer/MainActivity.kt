package dev.uthem.scriptplayer

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
import dev.uthem.scriptplayer.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 바 아이콘 색을 테마에 맞춰 준다. 이걸 빼면 밝은 배경에 흰 아이콘이 얹혀 안 보인다.
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppTheme.colors.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * 화면 사이 이동.
 *
 * 화면이 둘뿐이라 참·거짓 하나로 둔다. Navigation 을 얹으면 지금은 설정이 코드보다 많아진다.
 * 재생기와 설정이 들어오는 7·8단계에 제대로 바꾼다.
 */
@Composable
private fun AppRoot() {
    var addingScript by rememberSaveable { mutableStateOf(false) }

    if (addingScript) {
        // 뒤로 가기로 붙여넣기 화면을 닫는다. 없으면 앱이 통째로 닫힌다
        BackHandler { addingScript = false }
        AddScriptRoute(onDone = { addingScript = false })
    } else {
        LibraryRoute(onOpenAdd = { addingScript = true })
    }
}
