package dev.uthem.scriptplayer.ui.player

/**
 * 재생기 화면이 그리는 데 필요한 전부.
 *
 * 화면이 이 값만 받으므로, 스크린샷 테스트가 재생기·합성기 없이 어떤 상태든 그릴 수 있다 —
 * 재생 중·정지·합성 중·실패한 문장이 섞인 상태까지.
 */
data class PlayerUiState(
    val title: String = "",
    val sentences: List<PlayerSentence> = emptyList(),
    val currentIndex: Int = 0,
    /** 합성이 끝나 재생할 수 있는 문장 수. 전체와 같아지면 진행 표시가 사라진다 */
    val readySentences: Int = 0,
    val failedSentences: Set<Int> = emptySet(),
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val totalMs: Long = 0,
    val speed: Float = 1.0f,
    /**
     * 현재 문장을 따라 스크롤할지.
     *
     * 사용자가 손으로 스크롤하면 끈다 — 읽던 자리에서 화면이 끌려가면 아무것도 읽을 수 없다.
     */
    val followCurrent: Boolean = true,
) {
    val sentenceCount: Int get() = sentences.size
}

data class PlayerSentence(
    val text: String,
    val speakerLabel: String?,
)

/**
 * 미리보기와 스크린샷이 함께 쓰는 표본.
 *
 * 화자 넷, 아주 긴 문장 하나, 실패한 문장 하나를 섞는다. 짧고 성공한 문장만 두면
 * 줄이 넘칠 때와 실패를 알리는 모습을 한 번도 못 본다.
 */
internal fun previewPlayerState(
    playing: Boolean = true,
    synthesizing: Boolean = false,
): PlayerUiState {
    val sentences = listOf(
        PlayerSentence("자바스크립트 이벤트 루프, 한 번에 이해하기", null),
        PlayerSentence("안녕하세요. 오늘은 이벤트 루프를 다뤄보겠습니다.", "진행자"),
        PlayerSentence(
            "맞습니다. 자바스크립트는 한 번에 하나의 일만 처리합니다. 스레드가 하나거든요. " +
                "그런데 우리는 파일을 읽고, 네트워크를 기다리고, 사용자 입력을 받으면서도 " +
                "화면이 멈추지 않기를 기대하죠. 이 모순을 해결하는 장치가 이벤트 루프입니다.",
            "게스트",
        ),
        PlayerSentence("그래서 setTimeout(fn, 0) 도 기다려야 하는 거군요.", "진행자"),
        PlayerSentence("정확합니다. 타이머가 끝나면 콜백이 태스크 큐에 들어가 줄을 섭니다.", "게스트"),
        PlayerSentence("여기서 실무에서 문제가 되는 지점이 있을까요?", "청중"),
        PlayerSentence("마이크로태스크를 무한히 만들면 렌더링이 영원히 멈춥니다.", "전문가"),
    )
    return PlayerUiState(
        title = "자바스크립트 이벤트 루프, 한 번에 이해하기",
        sentences = sentences,
        currentIndex = 2,
        readySentences = if (synthesizing) 3 else sentences.size,
        failedSentences = if (synthesizing) emptySet() else setOf(5),
        playing = playing,
        positionMs = 42_000,
        totalMs = 187_000,
        speed = 1.25f,
    )
}
