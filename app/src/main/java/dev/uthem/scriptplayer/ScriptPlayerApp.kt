package dev.uthem.scriptplayer

import android.app.Application
import android.content.Context
import dev.uthem.scriptplayer.data.ScriptDatabase
import dev.uthem.scriptplayer.data.ScriptRepository

/**
 * 앱이 살아 있는 동안 하나만 있는 것들.
 *
 * Hilt 를 쓰지 않는다. 지금 담을 것이 데이터베이스와 보관함 둘뿐이고, 주입 틀을 얹으면
 * 코드보다 설정이 많아진다. 늘어나면 그때 바꾼다.
 */
class AppContainer(context: Context) {
    private val database by lazy { ScriptDatabase.open(context) }
    val scripts: ScriptRepository by lazy { ScriptRepository(database.scriptDao()) }
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
