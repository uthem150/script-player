# 대본 플레이어 — 팟캐스트 대본 재생기 설계

- 작성일: 2026-09-10
- 저장소: `RnD/script-player` (github.com/uthem150/script-player)
- 패키지: `dev.uthem.scriptplayer`
- 대상: 안드로이드 (minSdk 26, compileSdk 37.2, targetSdk 36)
- 스택: Kotlin 2.4.20, AGP 9.4.0, Compose BOM 2026.09.00, Media3 1.11.0,
  Room 2.8.5, Roborazzi 1.74.0, Robolectric 4.16.1

minSdk 26 은 타협이 아니라 요구다 — `UtteranceProgressListener.onRangeStart` 가
API 26 부터라, 단어 타이밍(§5.2)이 이 아래에서는 아예 오지 않는다.

---

## 1. 배경

이해가 안 되는 개념이나 구현 구조를 AI 에게 팟캐스트 대본으로 만들어 달라고 해서
듣는 습관이 있다. 대본은 나오지만 그걸 재생할 도구가 없다. 대본을 넣으면
사람 목소리로 읽어주고, 들으면서 속도를 조절하고, 놓친 대목을 되감을 수 있는 재생기가 필요하다.

핵심 사용 맥락은 **이동 중 폰, 화면 끄고, 에어팟으로 듣기**다. 그리고
"잠깐 뭐라고?" 하고 되감는 일이 자주 일어난다 — 개념 설명을 듣는 중이기 때문이다.

---

## 2. 범위

### 하는 것

- 대본을 붙여넣거나 다른 앱에서 공유해 받기
- 마크다운 잡음 제거, 화자 라벨 검출, 문장 분할
- 기기 내장 TTS 로 문장별 음성 파일 합성 (네트워크 불필요)
- 화면 끄고 백그라운드 재생, 잠금화면·알림 컨트롤, 에어팟·블루투스 버튼
- 재생 중 실시간 속도·음높이 조절
- 임의 지점 시크, 문장 단위 이전/다음, 이어듣기
- **본문에서 아무 단어나 탭하면 그 지점부터 이어서 재생**
- 현재 문장 하이라이트 + 자동 스크롤, 단어 하이라이트 (엔진이 타이밍을 줄 때)
- 대본 추가·수정·이름 변경·삭제
- 화자별 음성·음높이 배정

### 안 하는 것

- iOS, 데스크톱, 웹
- 서버, 로그인, 기기 간 동기화
- 클라우드 TTS API
- 대본 생성 (대본은 외부에서 만들어 온다)
- 연속재생 플레이리스트, 홈 위젯
- WAV → AAC 재인코딩 (1차 제외, 필요해지면 추가)

---

## 3. 결정 기록

설계 과정에서 확인한 사실과 그로부터 나온 결정이다. 나중에 "왜 이렇게 했나"를 다시
묻지 않기 위해 근거를 남긴다.

### 3.1 왜 웹이 아니라 네이티브인가

웹으로 시작했다가 접었다. `speechSynthesis` 로는 요구를 못 채운다.

- **iOS**: 화면이 잠기면 Safari 가 Web Audio 와 `speechSynthesis` 를 정지시킨다.
  발화 중에 백그라운드로 가면 페이지를 새로고침해야 복구된다. 무음 오디오 keepalive 도
  두 겹으로 막힌다 — 오디오 재생이 사용자 제스처와 같은 동기 호출 체인에 있어야 해서
  타이머로 재무장하는 keepalive 는 조용히 거부되고, AudioContext 자체가 제스처 사이에
  스스로 정지한다. iOS 는 `speechSynthesis` 를 "시스템 음성"으로 라우팅해 미디어 경로
  취급을 하지 않는다.
- **안드로이드**: 원인이 정책이 아니라 백그라운드 탭 스로틀링이라, 무음 오디오 루프로
  "미디어 재생 중"으로 인식시키는 우회로가 통할 여지는 있다. 하지만 보장이 없다.
- 결정적으로 `speechSynthesis` 에는 **파일로 합성하는 API 가 없다.** 아래 3.3 참고.

안드로이드 네이티브는 `ForegroundService` + `MediaSession` 으로 이 전부가 정식 경로다.
우회로도 트릭도 없다.

### 3.2 왜 안드로이드 전용인가

주 사용 기기가 안드로이드 폰이다. iOS 를 넣으면 Xcode 로 별도 앱을 또 만들어야 하고,
`AVSpeechSynthesizer` 에는 `synthesizeToFile` 대응물이 없어 (`write(_:toBufferCallback:)` +
수동 `AVAudioFile` 작성) 합성 경로를 따로 짜야 한다. 데스크톱 청취를 포기하는 대가로
안드로이드 하나를 제대로 만든다.

### 3.3 왜 미리 파일로 합성하는가

**"실시간 속도 조절" 요구가 이 구조를 강제한다.**

live `TextToSpeech.speak()` 는 속도를 바꾸면 현재 발화를 버리고 다시 읽어야 하고,
피치도 같이 변한다. 실시간이 아니다. 그리고 `TextToSpeech` 에는 pause 가 없다 (stop 뿐).

`TextToSpeech.synthesizeToFile()` 로 미리 WAV 를 만들어 두면 그 다음부터는 평범한
미디어 재생이 된다. ExoPlayer 의 `PlaybackParameters(speed, pitch)` 로 재생 중 즉시,
**피치를 유지한 채** 속도가 바뀐다. 완벽한 pause/resume, 임의 시크, 정확한 남은 시간이
전부 따라온다.

**따라서 합성은 항상 rate 1.0 으로 한다.** 합성 시점에 속도를 굽지 않는다 —
구우면 재생 시 배속과 곱해져 통제 불가가 된다. 속도는 재생기만의 관심사다.

피치는 다르다. 화자별 음높이 차이는 그 문장의 속성이므로 **합성 시점에 굽는다**.
사용자의 전역 음높이 조절은 재생 시점에 그 위로 곱해진다. 결과적으로
화자 구분은 유지되면서 전체 톤만 움직인다.

### 3.4 왜 문장별 파일 + 플레이리스트인가

대안은 대본 하나를 WAV 한 개로 이어붙이는 것이었다. 문장별을 택한 이유:

- **스트리밍 시작.** 첫 문장 합성이 끝나면 바로 재생을 시작하고 뒤는 백그라운드에서
  합성해 플레이리스트에 붙인다. 이어붙이기 방식은 자라는 파일을 재생해야 해서 취약하다.
- **WAV 수술이 없다.** 화자마다 다른 음성을 쓰면 샘플레이트가 다를 수 있어, 이어붙이려면
  리샘플링이 필요하다. 문장별 `MediaItem` 은 각자 독립이라 이 문제가 없다.
- **문장 인덱스가 공짜다.** `player.currentMediaItemIndex` 가 곧 문장 인덱스다.
  하이라이트 동기화에 별도 장부가 필요 없다. 이전/다음 문장은 `seekToNextMediaItem()`.

대가는 두 가지다. 문장 사이에 수십 ms 공백이 생기는데 팟캐스트 톤에서는 자연스럽다.
그리고 전체 진도 표시를 위해 항목 길이의 누적합을 직접 관리해야 한다 (§5.3).

**실측 후 덧붙임.** 합성이 실시간 대비 37.7배로 빨라서(§12) "아직 합성 안 된 뒤쪽으로
시크할 수 없다"는 제약이 거의 사라졌다. 10분 대본이 16초에 다 채워지므로 재생이
합성을 따라잡는 일이 없다. 스트리밍 시작은 그대로 두되, 이 제약을 UI 에서 설명할
필요는 없어졌다.

### 3.5 왜 Kotlin 이고 TypeScript 가 아닌가

확인한 사실:

- **`react-native-tts` 에 파일 합성 API 가 없다.** `speak()`, `voices()`,
  `setDefaultRate/Pitch`, `engines()`, `requestInstallData()` 와 4개 이벤트뿐이다.
  즉 TS 로 가도 `synthesizeToFile` Kotlin 브릿지는 직접 써야 한다.
- `react-native-track-player` 는 백그라운드·MediaSession·헤드셋·큐·`setRate()`·`seekTo()`
  를 지원해 재생 쪽은 완결된다. 단 v5 는 상업 라이선스(개인·교육 무료), v4 는 Apache-2.0.

TS 유지의 이득이 남지 않는 이유:

1. **어려운 부분이 어차피 Kotlin이다.** 브릿지가 놓이는 자리가 정확히 이 앱의 위험
   지점(TTS 합성, 오프라인 음성 감지, 파일 수명 관리)이다. 쉬운 UI 만 TS 로 쓰고
   디버깅할 곳은 Kotlin 으로 쓰면서 그 사이에 경계를 하나 더 놓는 셈이 된다.
2. **스크린샷 자동화.** Compose 에는 Roborazzi 가 있어 Robolectric 으로 **JVM 에서**
   화면을 렌더해 PNG 로 떨어뜨리고, Compose 테스트 API 로 클릭·스크롤까지 한 뒤 찍는다.
   에뮬레이터도 기기도 필요 없이 `./gradlew` 한 번에 유닛 테스트와 같이 돌고 결정론적이다.
   RN 에는 대응물이 없어 에뮬레이터 + Maestro/Detox 로 가야 하는데 느리고 잘 깨진다.
3. 이중 툴체인(Metro + Gradle)이 사라지고, 라이선스 판단도 필요 없다.

학습 비용은 실재하지만, 가장 큰 학습 대상인 파서가 순수 함수라 언어를 익히기에 가장
값싼 자리다.

### 3.6 왜 콘텐츠 주소 지정 캐시인가

오디오 파일명을 `sha256(문장 텍스트 + 음성 이름 + 피치 + 엔진 버전)` 으로 둔다.
이 한 가지 결정이 여러 요구를 동시에 푼다.

- 대본을 수정해도 **안 바뀐 문장은 파일을 그대로 재사용**한다. 전체 재합성이 필요 없다.
- 화자 배정만 바꾸면 **그 화자의 문장만** 재합성된다.
- 서로 다른 대본이 같은 문장을 담으면 파일 하나를 공유한다.
- 참조 카운트를 관리할 필요가 없다. 정리는 접근 시각 기준 LRU 로 충분하다.

---

## 4. 아키텍처

```text
┌──────────────────────────────────────────────┐
│ ui/  (Compose)                               │
│  library · player · settings · theme         │
└───────────────┬──────────────────────────────┘
                │ StateFlow
┌───────────────▼──────────────────────────────┐
│ domain/  ScriptSession · PlaybackFacade       │
└───┬───────────────┬──────────────┬───────────┘
    │               │              │
┌───▼─────┐  ┌──────▼──────┐  ┌───▼─────────┐
│ parser/ │  │ tts/        │  │ playback/   │
│ 순수    │  │ 합성 파이프  │  │ Media3      │
└─────────┘  └──────┬──────┘  └───┬─────────┘
                    │             │
              ┌─────▼─────────────▼─────┐
              │ data/  Room · AudioCache │
              └──────────────────────────┘
```

`share/` 는 `ACTION_SEND` 를 받아 `domain/` 으로 넘기는 얇은 진입점이다.

### 모듈 책임

| 패키지 | 하는 일 | 안드로이드 의존 |
|---|---|---|
| `parser/` | 원문 → 화자·문장 구조 | **없음** (순수 Kotlin) |
| `domain/` | 대본 수명주기, 합성·재생 조율 | 없음 (인터페이스만 의존) |
| `tts/` | `TextToSpeech` 격리, 문장 큐 합성 | 전부 여기 |
| `playback/` | `MediaSessionService`, ExoPlayer 플레이리스트 | 전부 여기 |
| `data/` | Room 보관함, 콘텐츠 주소 오디오 캐시 | Room, 파일시스템 |
| `ui/` | 화면 3개 + 디자인 시스템 | Compose |
| `share/` | `ACTION_SEND` 수신 | Intent |

**격리 원칙.** `parser/` 와 `domain/` 은 안드로이드 클래스를 import 하지 않는다.
`tts/` 와 `playback/` 은 각각 `Synthesizer`, `PlayerController` 인터페이스 뒤에 있어
테스트에서 fake 로 대체된다. 플랫폼 함정이 모듈 두 개 안에만 머문다.

---

## 5. 모듈 상세

### 5.1 `parser/` — 이 앱의 실제 값

순수 함수. 안드로이드 의존 0. TDD 로 가장 촘촘히 덮는 자산이다.

```kotlin
fun parseScript(raw: String): ParsedScript

data class ParsedScript(
    val title: String,          // 첫 heading, 없으면 첫 문장 앞부분
    val speakers: List<Speaker>,
    val sentences: List<Sentence>,
)

data class Speaker(val id: String, val label: String)

data class Sentence(
    val index: Int,
    val text: String,           // 읽을 텍스트 (라벨·마크업 제거됨)
    val speakerId: String?,     // null = 화자 없는 서술
    val sourceRange: IntRange,  // 원문 오프셋 — 하이라이트를 원문 위에 얹기 위해
)
```

**마크다운 정리**

| 대상 | 처리 |
|---|---|
| `#`~`######` 머리글 | 마커 제거, 본문은 문장으로 유지 |
| `**굵게**`, `*기울임*`, `` `코드` `` | 마커만 제거 |
| `[라벨](주소)` | 라벨만 남김 |
| ` ```코드블록``` ` | **통째로 제거** — 읽어봐야 소음 |
| `---`, `***` 구분선 | 제거 |
| `-`, `*`, `1.` 리스트 마커 | 제거, 항목은 각각 문장으로 |
| `> 인용` | 마커 제거 |
| 표 | **통째로 제거** — 셀을 이어 읽으면 뜻이 안 통한다 |

**화자 라벨 검출**

줄머리의 `이름:` 또는 `**이름**:` 패턴을 후보로 모은다. 이름은 1~12자.
전각 콜론(`：`)도 받는다.

> **같은 라벨이 2회 이상 나올 때만 화자로 인정한다.**
> `참고:`, `결론:`, `주의:` 처럼 한 번만 등장하는 건 화자가 아니다. 이 규칙 하나가
> 오검출을 거의 다 막는다.

화자로 인정된 라벨은 **읽지 않고** 화면에만 표시한다.

**문장 분할**

1. 줄바꿈은 항상 경계
2. `.` `?` `!` `…` 뒤 공백/줄끝을 경계로. 단 예외:
   - 소수점 (`3.14`)
   - 숫자 뒤 마침표 (`1. `) — 리스트 마커 단계에서 이미 제거되나 방어
   - 약어 (`e.g.`, `vs.`, `Mr.`, `etc.`)
   - 말줄임표 연속 (`...`)
3. 결과 문장이 200자를 넘으면 쉼표·접속 조사에서 보조 분할
   — TTS 엔진이 긴 입력에서 불안정해지는 것을 막는다
4. 빈 문장·공백만 남은 문장은 버린다

`sourceRange` 는 정리 전 원문 기준으로 보존한다. 화면은 원문을 보여주고 그 위에
하이라이트를 얹으므로, 정리된 텍스트 기준이면 위치가 어긋난다.

### 5.2 `tts/` — 합성 파이프라인

```kotlin
interface Synthesizer {
    /** 이 기기에서 쓸 수 있는 음성. 오프라인 가능한 것만. */
    suspend fun availableVoices(): List<VoiceInfo>
    /** 문장 하나를 캐시에 합성. 이미 있으면 그대로 반환. */
    suspend fun synthesize(request: SynthesisRequest): Result<SynthesizedSentence>
}

data class SynthesizedSentence(
    val audio: File,
    /** 글자 오프셋 → 시각. 엔진이 알려주지 않으면 빈 목록 (§5.2 단어 타이밍) */
    val wordTimings: List<WordTiming>,
)

data class WordTiming(val charStart: Int, val charEnd: Int, val atMs: Long)

data class SynthesisRequest(
    val text: String,
    val voiceName: String,
    val pitch: Float,   // 화자 구분용 — 합성 시점에 굽는다
)
// rate 는 없다. 합성은 항상 1.0 (§3.3)
```

**오프라인 음성 판별.** `TextToSpeech.voices` 에서 다음을 만족하는 것만 노출한다.

```kotlin
voice.locale.language == "ko" &&
!voice.isNetworkConnectionRequired &&
!voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
```

하나도 없으면 첫 실행 안내 화면에서 `ACTION_INSTALL_TTS_DATA` 로 시스템 설정에 보낸다.
(`TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`)

**완료 감지.** `synthesizeToFile` 은 즉시 반환하고 결과는
`UtteranceProgressListener.onDone/onError` 로 온다. `suspendCancellableCoroutine` 으로
감싸 호출 지점에서 `suspend fun` 으로 보이게 한다. utteranceId 는 캐시 키를 쓴다.

**화자별 음성 배정.** 실측(§12)에서 오프라인 한국어 음성이 **4종** 나왔다
(`ko-kr-x-ism/kob/koc/kod-local`). 그래서 화자마다 **실제로 다른 목소리**를 배정하는 것이
기본 경로다 — 검출된 화자 순서대로 음성을 돌려 붙인다.

화자가 음성 수보다 많거나 음성이 하나뿐인 기기에서는 **음높이로 구분**한다
(`1.0, 0.85, 1.15, 0.92 …`). 어느 쪽이든 사용자가 설정에서 직접 바꿀 수 있다.

**진행 상태.** 합성은 `Flow<SynthesisProgress>` 로 보고한다.

```kotlin
sealed interface SynthesisProgress {
    data class Done(val index: Int, val sentence: SynthesizedSentence) : SynthesisProgress
    data class Failed(val index: Int, val cause: Throwable) : SynthesisProgress
    data object Complete : SynthesisProgress
}
```

`Done` 이 나오는 즉시 `playback/` 이 플레이리스트에 붙인다. 이게 스트리밍 시작의 전부다.

**단어 타이밍.** `UtteranceProgressListener.onRangeStart(utteranceId, start, end, frame)` 가
합성 중에 지금 읽는 구간의 글자 오프셋과 **생성된 오디오의 프레임 위치**를 알려준다
(API 26+, `synthesizeToFile` 에도 온다). 이걸 모으면 문장 안의 글자 오프셋 → 시각 표가 된다.

```kotlin
atMs = frame * 1000L / sampleRate
```

이 표 하나가 두 가지를 가능하게 한다.

1. **아무 단어나 탭해서 그 지점부터 재생** (§5.6) — 비례 추정이 아니라 실제 값이다
2. **재생 중 현재 단어 하이라이트** — 문장 하이라이트 위에 얹는다

`sampleRate` 는 합성된 WAV 의 RIFF 헤더에서 읽는다. 엔진이 알려주는 값을 믿지 않는다 —
음성마다 다를 수 있다.

**모든 엔진이 `onRangeStart` 를 보내지는 않는다.** 안 오면 `wordTimings` 가 빈 목록이 되고,
탭 지점은 **글자 비율 × 문장 길이**로 추정한다. 문장이 200자 이하라 오차는 단어 한둘
수준이고, 단어 하이라이트는 이때 끄고 문장 하이라이트만 남긴다. 정확도가 떨어질 뿐
기능이 사라지지는 않게 한다.

### 5.3 `playback/` — Media3

`MediaSessionService` 하나가 ForegroundService·MediaSession·알림·미디어 버튼을 다 준다.
에어팟·블루투스 버튼을 위해 따로 할 일이 없다.

```kotlin
class PlaybackService : MediaSessionService() {
    // ExoPlayer + MediaSession.Builder(this, player).build()
}
```

**매니페스트**

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".playback.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

인터넷 권한은 **선언하지 않는다.** 네트워크를 쓰지 않는다는 것을 매니페스트로 못 박는다.

**실시간 조절**

```kotlin
player.playbackParameters = PlaybackParameters(speed, pitch)
```

속도 0.5~3.0, 음높이 0.7~1.3. 재생 중 즉시 반영되고 속도 변경 시 피치는 유지된다.

**전체 진도.** 문장별 항목이라 `player.currentPosition` 은 현재 항목 안의 위치다.
`Timeline` 에서 각 항목 길이를 읽어 누적합 배열을 만들어 둔다.

```text
전체 위치 = prefixSum[currentMediaItemIndex] + currentPosition
```

합성이 진행 중이면 아직 붙지 않은 문장의 길이를 모르므로, **글자 수 비례로 추정**해
채우고 실제 길이가 들어오면 교체한다. 진도 막대가 뒤로 튀지 않게 추정값은 항상
실측값보다 크지 않게 잡는다.

**컨트롤 매핑**

| 조작 | 동작 |
|---|---|
| 재생/일시정지 (에어팟 1탭, 알림, 잠금화면) | `play()` / `pause()` |
| 다음/이전 (에어팟 2·3탭) | 다음/이전 **문장** |
| 시크바 | 전체 위치로 이동 → 항목+오프셋으로 환산 |
| 10초 뒤로 | `seekBack()` — 개념 설명을 되감는 핵심 조작 |

`MediaMetadata` 의 title 에 현재 문장, artist 에 화자와 대본 제목을 넣어 잠금화면에서
어디쯤인지 보이게 한다.

`android.appKilledPlaybackBehavior` 는 앱을 스와이프로 닫으면 재생도 멈추게 둔다.

### 5.4 `data/`

**Room 보관함**

```kotlin
@Entity data class ScriptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val raw: String,             // 원문 — 재파싱·재합성의 원천
    val createdAt: Long,
    val updatedAt: Long,
    val lastSentenceIndex: Int,  // 이어듣기
    val lastPositionMs: Long,
    val speakerSettingsJson: String,
)
```

`Flow<List<ScriptEntity>>` 로 노출해 목록이 자동으로 갱신된다.
테스트는 인메모리 DB.

**오디오 캐시** (§3.6)

- 위치: `context.cacheDir/audio/`
- 파일명: `sha256(text + voiceName + pitch + engineVersion).wav`
- 같은 키의 `.json` 에 **단어 타이밍 표**를 함께 둔다 (§5.2). 오디오와 타이밍은
  같은 합성의 산물이라 수명을 함께 가야 한다 — 따로 관리하면 한쪽만 남아 어긋난다
- 총량 상한 **500MB**, 넘으면 접근 시각 오래된 것부터 삭제 (`.wav` 와 `.json` 을 한 쌍으로)
- `cacheDir` 이라 OS 가 공간 부족 시 지워도 되고, 지워지면 다시 합성된다
- WAV 는 크다. 실측(§12) 기준 **24kHz 16bit 모노 = 48KB/s → 10분 대본 약 29MB**.
  상한 500MB 면 10분 대본 17개쯤 담긴다. 지워져도 16초에 다시 만들어지니 충분하다.
  AAC 재인코딩은 1차 제외

### 5.5 `ui/` — 디자인 시스템

Compass 의 3계층 토큰을 Compose 로 이식한다. 계층 구조와 값을 유지하되, 폰 화면에
맞게 조정한다.

```text
theme/
  Primitive.kt   1계층 — object Primitive { val Blue500 = Color(0xFF3182F6) … }
  Alias.kt       2계층 — lightAliases / darkAliases 두 인스턴스
  Semantic.kt    3계층 — data class AppColors + LocalAppColors
  Theme.kt       AppTheme { … } — 컴포넌트는 오직 3계층만 참조
  Type.kt        Pretendard + 타입 스케일
```

**색은 Compass 값을 그대로** 쓴다 — Toss Blue(`#3182F6`) 계열, cool neutral gray,
그리고 다크 전용 `ink-*` 중성 회색. `ink` 를 따로 두는 이유는 Compass 주석에 남은
그대로다: 밝은 화면용 회색은 파랑이 섞여 있어 넓은 면을 어둡게 채우면 화면이
푸르스름해진다.

**타이포는 조정한다.** Compass 는 데스크톱 업무 도구라 본문이 13~14px 로 촘촘하다.
폰에서 읽으며 듣는 화면이므로:

- 본문 15px, 부가 정보 13px
- **대본 문장 18px, 행간 1.7** — 걸으면서도 읽히도록
- 현재 문장은 크기를 키우지 않는다 (레이아웃이 튀어 자동 스크롤이 어긋난다).
  배경과 굵기로만 강조

**반경**은 Compass보다 크게 (`radius-lg` 8→12px). 업무 도구가 아니라 듣는 앱이다.

**터치 대상은 최소 48dp.** 재생 컨트롤은 걷거나 흔들리는 상황에서 눌린다.

**시스템 바 인셋을 반드시 소비한다.** targetSdk 35 부터 앱이 화면 끝까지 그리는 것이
기본이라, 인셋을 처리하지 않으면 내용이 상태바와 뒤로가기 영역에 가려진다. 검증 앱에서
실제로 이걸 빠뜨려 아래쪽이 잘렸다. 화면마다 챙기지 않고 **루트 레이아웃 한 곳에서
`safeDrawingPadding()` 으로 처리**한다 — 화면마다 붙이면 언젠가 하나를 빠뜨린다.
스크롤 화면은 스크롤 뷰포트 자체에 인셋을 준다.

컴포넌트는 필요한 것만 만든다: `Button`, `IconButton`, `Card`, `Slider`, `Chip`,
`BottomSheet`, `TextField`. 각각 `@Preview` 와 Roborazzi 스크린샷 테스트를 함께 둔다.

### 5.6 화면

**보관함 (`LibraryScreen`)**

- 대본 카드 목록: 제목, 문장 수, 예상 길이, 진도 막대와 %, 마지막 들은 시각
- 카드 탭 → 재생기. 길게 눌러 → 이름 변경 / 삭제
- 우하단 FAB → 새 대본 (붙여넣기 시트)
- 비어 있을 때: 무엇을 붙여넣으면 되는지 예시를 보여준다

**재생기 (`PlayerScreen`)**

- 상단: 대본 제목, 뒤로, 설정 진입
- 본문: 원문이 문장 단위로 흐르고 현재 문장 하이라이트 + 자동 스크롤.
  화자 라벨은 문장 왼쪽에 칩으로.
  사용자가 손으로 스크롤하면 자동 스크롤을 멈추고 "현재 위치로" 버튼을 띄운다
- **아무 단어나 탭하면 그 지점부터 이어서 재생.** `TextLayoutResult.getOffsetForPosition`
  으로 탭 좌표를 글자 오프셋으로 바꾸고, 단어 타이밍 표(§5.2)에서 그 오프셋에 해당하는
  시각을 찾아 `seekTo(문장 인덱스, 그 시각)` 으로 간다. 타이밍 표가 없는 엔진에서는
  글자 비율로 추정한다.
  탭이 재생 위치 이동이므로, **텍스트 복사는 길게 눌러** 시스템 선택으로 한다
- 단어 타이밍이 있으면 재생 중 **현재 단어**를 문장 하이라이트 위에 얹어 표시.
  없으면 문장 하이라이트만
- 하단 컨트롤: 10초 뒤로 · 이전 문장 · **재생/정지(큼)** · 다음 문장 · 10초 앞으로
- 시크바 + 경과/전체 시간
- 속도 칩: `0.75 · 1.0 · 1.25 · 1.5 · 2.0` + 슬라이더로 미세 조절
- 합성 진행 중이면 상단에 얇은 진행 표시 ("문장 12/84 준비 중")

**설정 (`SettingsScreen`)**

- 음성 선택 (오프라인 가능한 것만 나열)
- 화자별 음성·음높이 배정 — 변경하면 해당 화자 문장만 재합성 (§3.6)
- 전역 음높이
- 오프라인 한국어 음성이 없으면 여기서 시스템 설정으로 보내는 안내
- 오디오 캐시 사용량과 비우기
- 테마 (시스템 / 밝게 / 어둡게)

### 5.7 `share/` — 공유 수신

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

이게 대본 전달 문제를 푼다. 데스크톱에서 대본 생성 → 카톡 나에게 보내기 → 폰에서
텍스트 길게 눌러 "공유 → 대본 플레이어" → 바로 파싱되어 보관함에 들어간다.

받은 텍스트는 즉시 저장하지 않고 **확인 시트**를 띄운다 — 제목과 검출된 화자를
보여주고, 사용자가 확인하면 저장한다. 잘못 던진 텍스트가 조용히 쌓이는 것을 막는다.

---

## 6. 오류 처리

| 상황 | 처리 |
|---|---|
| 오프라인 한국어 음성 없음 | 안내 화면 + 시스템 설정 딥링크. 앱을 못 쓰는 상태임을 분명히 |
| `synthesizeToFile` 이 특정 문장에서 실패 | 그 문장을 건너뛰고 계속. 화면에 표시하고 재시도 버튼 |
| `synthesizeToFile` 을 엔진이 아예 지원 안 함 | live `speak()` 폴백 모드로 전환 — 실시간 속도·시크는 불가함을 알린다 |
| 저장 공간 부족 | LRU 로 캐시를 비우고 재시도, 그래도 안 되면 안내 |
| 캐시 파일이 외부에서 삭제됨 | 재생 시 없으면 재합성 |
| 엔진이 `onRangeStart` 를 안 보냄 | 탭 지점을 글자 비율로 추정, 단어 하이라이트만 끈다 |
| 오디오는 있는데 타이밍 JSON 이 없음 | 타이밍 없는 것으로 취급 (재합성하지 않는다 — 소리는 멀쩡하다) |
| 붙여넣은 텍스트가 비었거나 문장이 0개 | 저장하지 않고 이유를 알린다 |
| 대본이 매우 길다 (1000문장 이상) | 저장은 하되 합성을 앞쪽부터 점진 진행. 경고 표시 |
| TTS 엔진이 재생 중 죽음 | 세션 재초기화 후 마지막 위치에서 재개 |

**폴백 모드는 단순히 두 번째 경로가 아니다.** `PlayerController` 인터페이스를 두 구현이
만족하게 해서, UI 는 어느 쪽인지 모르게 한다. 다만 폴백일 때 시크바와 속도 슬라이더는
비활성으로 그린다.

---

## 7. 테스트 전략

TDD 로 진행한다. 테스트를 먼저 쓰고 실패를 보고 나서 구현한다.

### 7.1 유닛 테스트 (JVM, 에뮬레이터 없음)

**`parser/` — 가장 두꺼운 자산.** 순수 함수라 전부 덮는다.

- 마크다운: 머리글·강조·링크·구분선·리스트·인용 각각, 코드블록 제거, 표 제거
- 화자 검출: 정상 2인 대화, `**이름**:` 형태, 전각 콜론, **1회만 등장하는 라벨은
  화자가 아님**, 이름이 12자 초과, 콜론이 문장 중간에 있는 경우
- 문장 분할: 종결부호, 소수점, 약어, 말줄임표, 200자 초과 보조 분할, 빈 문장 제거
- `sourceRange` 가 원문 오프셋과 실제로 일치하는지 (하이라이트 정확도의 근거)
- 실제 AI 생성 대본 3~4개를 픽스처로 두고 회귀 방지

**`domain/`** — fake `Synthesizer` + fake `PlayerController` 로 조율 로직 검증.
합성 완료 순서가 뒤섞여 와도 플레이리스트 순서가 맞는지, 이어듣기 지점이 저장되는지,
화자 설정 변경 시 재합성 대상이 그 화자 문장뿐인지.

**`data/`** — 인메모리 Room, 콘텐츠 주소 캐시 키 안정성, LRU 정리 경계.

**`tts/` · `playback/`** — 인터페이스 계약 테스트. 실제 `TextToSpeech`·ExoPlayer 를
쓰는 부분은 유닛 테스트 대상이 아니다 (§7.3 에서 실기기로 검증).

### 7.2 스크린샷 테스트 — Roborazzi

Compass 의 `screenshot.mjs` 가 하는 일을 옮긴다. Robolectric 으로 JVM 에서 렌더하므로
에뮬레이터가 필요 없고 결정론적이다.

```bash
./gradlew recordRoborazziDebug   # PNG 기록
./gradlew verifyRoborazziDebug   # 회귀 검증 (CI)
```

**찍는 축**

- 화면: 보관함(빈 상태·목록), 재생기(재생 중·정지·합성 중·폴백 모드), 설정,
  공유 확인 시트, 음성 없음 안내
- 테마: 밝게 × 어둡게
- 상태: 긴 제목, 화자 4명, 문장 하나가 매우 긴 경우, 진도 0%·중간·100%

**Compass 하네스에서 가져올 교훈** — 그 파일 주석에 실패 경험이 남아 있다.

1. **깨진 화면을 찍고 지나치지 않는다.** Compass 는 오류 화면을 런타임에 감지했는데,
   Roborazzi 는 더 강하게 할 수 있다 — 캡처 **전에** 기대하는 내용이 실제로 있는지
   `assertExists()` 로 단언한다. 없으면 테스트가 실패하므로 깨진 스크린샷이 기록될 수 없다.
2. **테마를 못 박는다.** Compass 는 OS 선호만 바꿨다가 저장된 값에 밀려 다크 촬영이
   전부 밝게 나온 적이 있다. Roborazzi 에서는 테마를 컴포저블 파라미터로 직접 넘긴다.
3. **비어 있는 화면은 아무것도 증명하지 않는다.** 목록이 비면 그 줄이 깨져도 알 수 없다.
   픽스처에 길이가 제각각인 제목, 화자 여러 명, 칸을 넘치는 문장을 섞는다.
4. **고정 날짜를 쓰지 않는다.** Compass 는 날짜를 못 박아 두었다가 그날이 지난 뒤로
   달력이 늘 비어 찍혔다. 상대 시각으로 만든다.

**샘플 데이터**는 실제 AI 생성 대본을 픽스처로 쓴다 — 이벤트 루프, 렌더링 파이프라인
같은 실제로 듣고 싶은 주제. 파서 테스트와 픽스처를 공유한다.

### 7.3 실기기 체크리스트

자동화할 수 없는 것들이다. 릴리스 전 수동으로 확인한다.

- [ ] 오프라인 한국어 음성으로 합성됨 (**비행기 모드**에서 전 과정)
- [ ] 화면 끄고 끝까지 재생 유지
- [ ] 에어팟 1탭 = 재생/정지, 2탭 = 다음 문장, 3탭 = 이전 문장
- [ ] 잠금화면 컨트롤에 현재 문장·화자 표시, 시크 동작
- [ ] 다른 앱에서 텍스트 공유 → 확인 시트 → 저장
- [ ] 재생 중 속도 변경이 즉시 반영되고 피치가 유지됨
- [ ] 전화가 오면 멈추고, 끊으면 재개 (오디오 포커스)
- [ ] 블루투스 연결 해제 시 정지
- [ ] 앱을 스와이프로 닫으면 재생도 멈춤
- [ ] 이어듣기 지점이 앱 재시작 후에도 유지
- [ ] 합성 실측 속도 기록 (10분 분량 대본이 몇 초에 준비되는지)

### 7.4 CI

`./gradlew check` 가 유닛 테스트 + Roborazzi 검증 + ktlint + Android Lint 를 돈다.
실기기 항목은 CI 대상이 아니므로 릴리스 체크리스트로 관리한다.

---

## 8. README 자동 생성

Compass 처럼 실제 화면을 담은 README 를 만들되, 스크린샷을 손으로 갱신하지 않는다.

`./gradlew updateReadme` 가:

1. `recordRoborazziDebug` 로 PNG 를 기록
2. `docs/screens/` 로 README 에 실을 것만 골라 복사 (공개용은 별도 축 — 큰 기기
   qualifier, 픽스처가 풍성한 상태)
3. `README.md` 의 마커 블록 사이를 이미지 표로 다시 쓴다

```markdown
<!-- SCREENS:BEGIN -->
(생성된 표)
<!-- SCREENS:END -->
```

마커 밖의 글은 손으로 쓴다. 생성기가 사람이 쓴 설명을 덮지 않게 하는 경계다.

스크린샷이 실제 테스트 통과의 산물이므로, **README 에 실린 화면은 정의상 깨지지 않은
화면**이다. Compass 에서 네 번 겪은 "멀쩡한 줄 알았던 화면"이 구조적으로 불가능해진다.

---

## 9. 위험과 대응

| 위험 | 상태 | 대응 |
|---|---|---|
| 기기에 오프라인 한국어 음성 없음 | **해소** — 실측 기기에 4종 있음 | 다른 기기를 위해 첫 실행 감지 + 설정 딥링크는 그대로 만든다 |
| 합성이 느림 | **해소** — 실시간 37.7배 | 스트리밍 시작은 유지 |
| `onRangeStart` 미수신 | **해소** — 313건 수신 | 안 오는 엔진용 추정 경로는 남긴다 |
| 에어팟 1탭 미수신 | **해소** — 재생/정지 확인 | — |
| 에어팟 2·3탭으로 문장 이동 | 미확인 | `spike-2` 로 플레이리스트를 실제로 걸고 재측정 |
| 화면 끈 뒤 연속 재생 | 미확인 | `spike-2` 에서 확인 |
| 비행기 모드 합성 | 미확인 (오프라인 음성으로 합성됐으니 유력) | `spike-2` 에서 확인 |
| 네트워크 음성을 골라 조용히 실패 | 실재 — `-local`/`-network` 가 짝으로 있음 | §5.2 세 조건 필터. 필터 자체를 테스트로 고정 |
| `synthesizeToFile` 부실한 엔진 | 다른 기기의 위험 | live `speak()` 폴백 모드 |
| WAV 용량 (10분 29MB) | 실재 | 500MB 상한 + LRU, `cacheDir` 사용 |
| 시스템 바 인셋 누락 | 실재 — 검증 앱에서 겪음 | 루트 레이아웃 한 곳에서 처리 (§5.5) |
| 문장 수백 개 플레이리스트 성능 | 미확인 | 1000문장 픽스처로 측정 |
| Kotlin·Compose 학습 | 진행 중 | 파서(순수 함수)부터 시작해 플랫폼 API 는 나중에 |

---

## 10. 환경 준비

설치 절차는 [README](../../../README.md#4-만들고-돌리기) 에 있다. 설치하면서 걸린 것을
남긴다 — 다시 겪지 않도록.

| 걸린 것 | 사실 |
| --- | --- |
| JDK | JDK 25 는 AGP 가 아직 안 받는다. **21** 을 쓴다. cask `temurin@21` 은 `.pkg` 라 sudo 를 물어 멈추므로 formula `openjdk@21` 로 받는다 |
| Kotlin 플러그인 | **AGP 9 부터 Kotlin 지원이 내장.** `kotlin-android` 를 적용하면 빌드가 거부된다 |
| 플랫폼 이름 | 마이너 버전까지 쓴다. `platforms;android-37` 은 **없고** `android-37.2` 가 있다 |
| compileSdk 표기 | `compileSdk = 37` + `compileSdkMinor = 2` 로 나눠 적는다 |
| compileSdk 하한 | 지금 androidx 스택이 **37 이상**을 요구한다. 36 으로는 의존성 15개가 거부한다 |
| Gradle | 래퍼로 받는다. AGP 9.4.0 은 Gradle **9.7.1** 로 확인했다 |

배포는 Play Store 없이 `./gradlew assembleDebug` → APK 직접 설치.

---

## 11. 구현 순서

각 단계는 테스트가 통과하고 화면을 눈으로 확인한 뒤에 다음으로 넘어간다.

**1단계 · 환경과 미지수 검증**
Android SDK 설치, 프로젝트 골격. 화면 하나뿐인 검증 앱으로 세 가지를 실측한다 —
오프라인 한국어 음성 존재 여부, `synthesizeToFile` 속도, `MediaSessionService` 로
에어팟 탭 수신. **결과를 이 스펙에 반영한 뒤** 2단계로 간다. 여기서 뒤집히면 구조가
바뀌므로 본 구현 전에 확인한다.

**2단계 · 디자인 시스템과 스크린샷 파이프라인**
토큰 3계층 이식, 기본 컴포넌트, Roborazzi 설정. 이 단계 끝에 첫 PNG 가 나오고
`updateReadme` 가 돌아야 한다. 파이프라인을 먼저 세우면 이후 모든 화면이 자동으로
검증 대상이 된다.

**3단계 · 파서 (TDD)**
순수 Kotlin. 테스트를 먼저 쓴다. 실제 대본 픽스처로 회귀 방지.

**4단계 · 보관함**
Room, 대본 CRUD, 공유 수신. 화면과 스크린샷 테스트 함께.

**5단계 · 합성 파이프라인**
`Synthesizer`, 콘텐츠 주소 캐시, LRU, 진행 상태 Flow.

**6단계 · 재생**
`MediaSessionService`, 플레이리스트 스트리밍, 실시간 속도·음높이, 시크, 진도 누적합.

**7단계 · 재생기 화면**
하이라이트, 자동 스크롤, 컨트롤. 상태별 스크린샷 전부.

**8단계 · 설정과 마무리**
음성·화자 배정, 캐시 관리, 테마. 실기기 체크리스트 완주. README 생성.

---

## 12. 1단계 실측 결과

측정 기기: **Galaxy S23+ (SM-S916N) · Android 16 (SDK 36)** · Google TTS
(`com.google.android.tts`). 검증 앱(`spike-1`)으로 실기기에서 쟀다.

| 재려던 것 | 결과 | 설계에 미친 영향 |
| --- | --- | --- |
| 오프라인 한국어 음성 | **5개** (`ko-KR-language`, `ko-kr-x-ism/kob/koc/kod-local`) | 예상을 넘음. **화자별로 실제 다른 목소리를 배정**한다. 음높이 구분은 폴백으로 내려감 |
| 합성 속도 | 41문장 1300자 → **4.79초**, 오디오 180초 = **실시간 대비 37.7배** | 10분 대본이 약 16초. 시크 범위가 합성 진도에 묶이는 우려가 사실상 사라짐 |
| `onRangeStart` | **313건 수신** | 단어 탭 재생을 정확값으로, 단어 하이라이트도 확정 |
| 미디어 버튼 | 에어팟 **1탭 = 재생/정지 확인** | Media3 `MediaSessionService` 로 충분. 우회로 불필요 |
| 샘플레이트 | **24000Hz** 16bit 모노 = 48KB/s | 용량 계산 갱신 (§5.4) |

전체 음성은 474개인데 한국어는 9개이고 그중 `-network` 접미사 4개가 네트워크를 요구한다.
**같은 화자 이름에 `-local` 과 `-network` 가 짝으로 있다** — 필터를 안 걸면 네트워크
음성을 골라 비행기 모드에서 조용히 실패한다. §5.2 의 세 조건 필터가 그래서 필요하다.

### 아직 확인 못 한 것

첫 검증 앱이 소리 하나만 재생해서, 다음·이전이 갈 곳이 없어 확인되지 않았다.
합성한 문장들을 실제 플레이리스트로 재생하는 `spike-2` 로 다시 잰다.

- 에어팟 2탭·3탭으로 문장 이동
- 화면을 끈 뒤 연속 재생
- 비행기 모드에서 합성
- 1000문장 플레이리스트 성능
