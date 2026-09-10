package dev.uthem.scriptplayer.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import java.io.File

/**
 * Media3 로 [PlayerController] 를 구현한다.
 *
 * 서비스가 실제 재생기를 들고 있고, 화면은 [MediaController] 로 그것을 조작한다.
 * 이 클래스는 그 사이의 얇은 껍데기다 — 조율 로직은 [PlaybackSession] 에 있고
 * 기기 없이 테스트된다.
 */
@OptIn(UnstableApi::class)
class Media3PlayerController(private val controller: MediaController) : PlayerController {

    // setScriptTitle 과 JVM 시그니처가 겹치지 않게 이름을 달리 둔다
    private var currentScriptTitle: String = ""
    private var speed: Float = 1.0f
    private var pitch: Float = 1.0f

    override val currentIndex: Int get() = controller.currentMediaItemIndex
    override val positionMs: Long get() = controller.currentPosition.coerceAtLeast(0)
    override val isPlaying: Boolean get() = controller.isPlaying
    override val itemCount: Int get() = controller.mediaItemCount

    override fun setScriptTitle(title: String) {
        currentScriptTitle = title
    }

    override fun append(index: Int, audio: File, sentenceText: String) {
        controller.addMediaItem(
            MediaItem.Builder()
                .setUri(Uri.fromFile(audio))
                .setMediaId("$index")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        // 잠금화면 첫 줄 — 지금 읽는 문장
                        .setTitle(sentenceText.ifBlank { "문장 ${index + 1}" })
                        .setArtist(currentScriptTitle)
                        .build(),
                )
                .build(),
        )
        // 첫 항목이 들어왔을 때만 준비한다. 매번 부르면 재생 중에 끊긴다
        if (controller.mediaItemCount == 1) controller.prepare()
    }

    override fun play() = controller.play()

    override fun pause() = controller.pause()

    override fun seekTo(sentenceIndex: Int, withinMs: Long) {
        /*
         * 아직 붙지 않은 문장으로는 갈 수 없다.
         *
         * 합성이 재생보다 훨씬 빠르므로 드문 일이지만, 시크바를 끝까지 끌면 일어난다.
         * 그때 범위 밖으로 부르면 Media3 가 던진다 — 붙어 있는 마지막으로 자른다.
         */
        val target = sentenceIndex.coerceIn(0, maxOf(0, controller.mediaItemCount - 1))
        controller.seekTo(target, withinMs.coerceAtLeast(0))
    }

    /**
     * 속도만 바꾸고 음높이는 유지한다.
     *
     * 두 값을 함께 넘겨야 하는 API 라, 바꾸지 않는 쪽은 지금 값을 다시 넣는다.
     * 하나만 넘기면 다른 쪽이 1.0 으로 되돌아간다.
     */
    override fun setSpeed(speed: Float) {
        this.speed = speed
        applyParameters()
    }

    override fun setPitch(pitch: Float) {
        this.pitch = pitch
        applyParameters()
    }

    private fun applyParameters() {
        controller.playbackParameters = PlaybackParameters(speed, pitch)
    }

    override fun clear() {
        controller.clearMediaItems()
    }
}
