package dev.uthem.scriptplayer.playback

import java.io.File

/**
 * 재생기를 감싼 인터페이스.
 *
 * ExoPlayer 를 이 뒤에 두어, 조율 로직(합성 진행에 맞춰 붙이기·시크 환산·이어듣기 저장)을
 * 기기 없이 테스트한다. 실제 재생은 기기에서만 확인할 수 있지만, **언제 무엇을 부르는지**는
 * 여기서 못 박을 수 있고 버그는 대개 그쪽에 있다.
 */
interface PlayerController {
    /**
     * 지금 재생 중인 **재생목록 항목 번호**. 아직 아무것도 없으면 0.
     *
     * 문장 번호와 다를 수 있다 — 이어듣기로 열면 재생목록이 그 문장부터 시작하기 때문이다.
     * 둘을 잇는 것은 [PlaybackSession] 이 한다.
     */
    val currentIndex: Int

    /** 지금 문장 안에서의 위치. */
    val positionMs: Long

    val isPlaying: Boolean

    /** 붙어 있는 문장 수. 합성이 진행되며 늘어난다. */
    val itemCount: Int

    /**
     * 대본 제목. 잠금화면 컨트롤에 함께 뜬다.
     *
     * 문장마다 넣지 않고 한 번만 정한다 — 문장이 바뀔 때마다 제목까지 다시 쓰면
     * 알림이 깜빡인다.
     */
    fun setScriptTitle(title: String)

    /**
     * 목록 끝에 문장 하나를 붙인다.
     *
     * [sentenceText] 는 잠금화면에 띄울 글이다. 화면을 끈 채 들을 때 지금 어디쯤인지
     * 알 수 있는 유일한 창구다.
     */
    fun append(index: Int, audio: File, sentenceText: String)

    fun play()

    fun pause()

    /** 특정 **재생목록 항목**의 특정 위치로 옮긴다. */
    fun seekTo(itemIndex: Int, withinMs: Long)

    /**
     * 재생 속도와 음높이.
     *
     * 재생 중에 즉시 바뀌고, 속도를 바꿔도 음높이는 유지된다 — 파일에 배속을 굽지 않고
     * 재생기에서 조절하기 때문이다.
     */
    fun setSpeed(speed: Float)

    fun setPitch(pitch: Float)

    /** 전부 비운다. 다른 대본으로 옮길 때 쓴다. */
    fun clear()
}
