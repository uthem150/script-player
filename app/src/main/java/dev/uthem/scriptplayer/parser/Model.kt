package dev.uthem.scriptplayer.parser

/**
 * 대본을 읽을 수 있는 조각으로 나눈 결과.
 *
 * 이 패키지는 안드로이드 클래스를 하나도 import 하지 않는다 — 순수 함수라 JVM 테스트로
 * 전부 덮을 수 있고, 이 앱에서 가장 촘촘히 덮어야 하는 자리다.
 */
data class ParsedScript(
    val title: String,
    val speakers: List<Speaker>,
    val sentences: List<Sentence>,
    /**
     * 제목이 머리글에서 왔는지.
     *
     * 머리글에서 왔으면 첫 문장과 제목이 같은 글자다 — 머리글도 읽히는 문장이기 때문이다.
     * 머리글이 없을 때는 첫 문장 앞부분을 제목으로 삼으므로, 같아 보여도 첫 문장을
     * 제목의 중복으로 취급하면 실제 내용을 잃는다. 그 둘을 구분하려면 이 값이 필요하다.
     */
    val titleFromHeading: Boolean,
)

/**
 * 대본에 나오는 화자.
 *
 * [id] 는 합성 캐시 키와 설정에 쓰이므로 라벨이 아니라 안정적인 값이어야 한다 —
 * 라벨을 그대로 쓰면 "진행자" 를 "호스트" 로 고쳤을 때 배정이 전부 풀린다.
 */
data class Speaker(val id: String, val label: String)

data class Sentence(
    val index: Int,
    /** 실제로 읽을 글자. 화자 라벨과 마크다운 기호가 벗겨진 상태 */
    val text: String,
    /** null 이면 화자 없는 서술 */
    val speakerId: String?,
    /**
     * 이 문장이 **원문에서** 차지하는 범위.
     *
     * 화면은 원문을 보여주고 그 위에 하이라이트를 얹는다. 정리된 글자 기준으로 잡으면
     * 마크다운 기호가 빠진 만큼 위치가 밀려, 단어를 탭했을 때 엉뚱한 데서 재생된다.
     */
    val sourceRange: IntRange,
)
