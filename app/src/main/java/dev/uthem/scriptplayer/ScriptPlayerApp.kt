package dev.uthem.scriptplayer

import android.app.Application
import android.content.Context
import dev.uthem.scriptplayer.data.AudioCache
import dev.uthem.scriptplayer.data.ScriptDatabase
import dev.uthem.scriptplayer.data.ScriptRepository
import java.io.File

/**
 * 앱이 살아 있는 동안 하나만 있는 것들.
 *
 * Hilt 를 쓰지 않는다. 지금 담을 것이 데이터베이스와 보관함 둘뿐이고, 주입 틀을 얹으면
 * 코드보다 설정이 많아진다. 늘어나면 그때 바꾼다.
 */
class AppContainer(private val context: Context) {
    private val database by lazy { ScriptDatabase.open(context) }
    val scripts: ScriptRepository by lazy { ScriptRepository(database.scriptDao()) }

    /**
     * 합성한 소리를 담는 곳.
     *
     * `cacheDir` 에 둔다 — 공간이 모자라면 OS 가 먼저 지워도 되고, 지워져도 다시 만들면
     * 된다(실측에서 10분 대본이 18초였다). 사용자 자료가 아니므로 백업 대상도 아니다.
     */
    val audioCache: AudioCache by lazy { AudioCache(File(context.cacheDir, "audio")) }
}

class ScriptPlayerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** 화면에서 컨테이너를 꺼내는 창구. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ScriptPlayerApp).container
