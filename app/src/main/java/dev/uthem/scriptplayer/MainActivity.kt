package dev.uthem.scriptplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.uthem.scriptplayer.ui.theme.AppTheme
import dev.uthem.scriptplayer.ui.theme.Space

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
                    Placeholder()
                }
            }
        }
    }
}

/** 2단계 나머지(컴포넌트)와 4단계 보관함 화면이 이 자리를 차지한다. */
@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // targetSdk 35+ 는 화면 끝까지 그리는 것이 기본이라, 인셋을 소비하지 않으면
            // 내용이 상태바와 뒤로가기 영역에 가려진다. 루트 한 곳에서만 처리한다.
            .safeDrawingPadding()
            .padding(Space.x6),
        verticalArrangement = Arrangement.spacedBy(Space.x2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "대본 플레이어",
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary,
        )
        Text(
            text = "디자인 시스템 이식 중",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun PlaceholderLightPreview() {
    AppTheme(darkTheme = false) {
        Surface(color = AppTheme.colors.background) { Placeholder() }
    }
}

@Preview
@Composable
private fun PlaceholderDarkPreview() {
    AppTheme(darkTheme = true) {
        Surface(color = AppTheme.colors.background) { Placeholder() }
    }
}
