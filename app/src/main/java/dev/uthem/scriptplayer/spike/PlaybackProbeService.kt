package dev.uthem.scriptplayer.spike

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/*
 * 1단계 검증 앱 전용. 실측이 끝나면 spike 패키지째로 지운다.
 *
 * 확인하려는 것: 에어팟·헤드셋 버튼이 이 앱까지 들어오는가, 화면을 끈 뒤에도 계속 도는가.
 *
 * MediaSessionService 하나가 포그라운드 서비스·미디어 세션·알림·미디어 버튼을 다 준다.
 * 웹에서 필요했던 무음 앵커 같은 장치가 여기서는 필요 없다.
 *
 * 버튼이 들어왔는지는 **재생 상태 변화**로 판단한다. 원시 키 이벤트를 가로채는 쪽이
 * 더 자세하지만, 정작 알고 싶은 것은 "눌렀을 때 실제로 멈추는가" 이고 그건 이쪽이 답한다.
 */
@OptIn(UnstableApi::class)
class PlaybackProbeService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val where = "문장 ${player.currentMediaItemIndex + 1}/${player.mediaItemCount}"
                    ProbeLog.record(if (isPlaying) "▶ 재생 · $where" else "⏸ 정지 · $where")
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val where = "문장 ${player.currentMediaItemIndex + 1}/${player.mediaItemCount}"
                    ProbeLog.record("↔ $where · ${reasonLabel(reason)}")
                }

                override fun onPlayerError(error: PlaybackException) {
                    ProbeLog.record("✖ 재생 오류: ${error.errorCodeName}")
                }
            },
        )
        session = MediaSession.Builder(this, player).build()
        ProbeLog.record("서비스 시작")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.let { active ->
            active.player.release()
            active.release()
        }
        session = null
        ProbeLog.record("서비스 종료")
        super.onDestroy()
    }
}

/**
 * 이동 이유를 읽을 수 있게 적는다.
 *
 * 앞선 실측에서 `reason=3` 만 찍혀 에어팟 두 번 탭이 들어온 것으로 보였는데, 실제로는
 * 목록을 새로 넣어서(PLAYLIST_CHANGED) 난 것이었다. 숫자만 남기면 이렇게 잘못 읽는다.
 * 버튼으로 넘긴 것은 SEEK 로 찍힌다.
 */
private fun reasonLabel(reason: Int): String = when (reason) {
    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "반복"
    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "자동으로 다음"
    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "버튼·시크로 이동"
    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "목록을 새로 넣음"
    else -> "reason=$reason"
}
