package dev.uthem.scriptplayer.playback

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 재생을 담당하는 포그라운드 서비스.
 *
 * 이 클래스 하나가 포그라운드 서비스·미디어 세션·알림·미디어 버튼을 다 준다. 그래서
 * 화면을 끈 뒤에도 계속 돌고, 에어팟 버튼이 앱까지 들어온다 — 1단계에서 실기기로 확인했다.
 *
 * 웹에서 필요했던 무음 앵커 같은 장치가 여기서는 없다. 안드로이드의 정식 경로이기 때문이다.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        /*
         * 이어지는 재생을 끝에서 멈춘다.
         *
         * 반복으로 두면 대본이 끝나고 처음으로 돌아가, 자다가 다시 듣게 된다.
         */
        player.repeatMode = Player.REPEAT_MODE_OFF
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * 앱을 스와이프로 닫으면 재생도 멈춘다.
     *
     * 계속 돌게 두면 앱을 닫았는데 소리가 나는 상태가 되어, 멈추려면 알림을 찾아야 한다.
     */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        session?.player?.pause()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.let { active ->
            active.player.release()
            active.release()
        }
        session = null
        super.onDestroy()
    }
}
