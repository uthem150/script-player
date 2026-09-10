# 대본 플레이어

팟캐스트 대본을 넣으면 읽어주는 안드로이드 재생기.

이해가 안 되는 개념을 AI 에게 팟캐스트 대본으로 만들어 달라고 하는 습관이 있는데,
대본은 나와도 그걸 재생할 도구가 없었습니다. 대본을 던져 넣으면 사람 목소리로 읽어주고,
걸으면서 속도를 올리고, 놓친 대목을 되감을 수 있는 재생기입니다.

> 만드는 사람 기준: **화면을 끄고 에어팟으로** 들을 수 있어야 하고,
> 인터넷이 없어도 돌아야 합니다. 둘 중 하나라도 못 지키면 쓸 이유가 없습니다.

---

## 1. 무엇을 하나

| 화면 | 하는 일 |
| --- | --- |
| **보관함** | 대본을 모아 두고 어디까지 들었는지 보여준다. 추가·이름 변경·삭제 |
| **재생기** | 현재 문장을 짚어가며 재생. 되감기·문장 이동·속도 조절·시크 |
| **설정** | 음성 고르기, 화자별 음성·음높이 배정, 캐시 관리, 테마 |

다른 앱에서 텍스트를 **공유**하면 바로 들어옵니다 — 데스크톱에서 만든 대본을
메신저로 자신에게 보내고, 폰에서 길게 눌러 "공유 → 대본 플레이어" 로 던져 넣는 흐름입니다.

재생 중에 **속도와 음높이가 즉시 바뀝니다.** 속도를 올려도 목소리 톤은 그대로입니다.
대본을 미리 음성 파일로 합성해 두기 때문에, 듣는 동안은 평범한 음악 재생과 같습니다.
그래서 잠금화면 컨트롤과 에어팟 버튼이 정식으로 동작하고, 놓친 대목을 10초 되감을 수 있습니다.

화자가 둘 이상인 대본은 화자별로 다른 목소리(또는 음높이)로 읽어 실제 대담처럼 들립니다.
마크다운 기호와 코드 블록은 읽지 않고 걸러냅니다.

---

## 2. 인터넷이 필요 없음

네트워크 권한을 **아예 선언하지 않습니다.** 기기에 설치된 TTS 음성으로 합성하므로
비행기 모드에서도 전 과정이 돕니다.

한 가지 전제가 있습니다 — 기기에 **오프라인 한국어 음성 데이터**가 있어야 합니다.
없으면 첫 실행에서 알려주고 시스템 설정으로 안내합니다
(설정 → 언어 및 입력 → 음성 합성).

---

## 3. 화면

화면이 전부 모였습니다. 아래 사진은 모두 통과한 테스트의 산출물입니다.

<!-- SCREENS:BEGIN -->

| | 밝게 | 어둡게 |
| --- | --- | --- |
| **재생기** | <img src="app/screenshots/player-light.png" width="240" alt="재생기 밝게"> | <img src="app/screenshots/player-dark.png" width="240" alt="재생기 어둡게"> |
| **재생기 · 정지와 합성 중** | <img src="app/screenshots/player-paused-light.png" width="240" alt="재생기 · 정지와 합성 중 밝게"> | <img src="app/screenshots/player-synthesizing-dark.png" width="240" alt="재생기 · 정지와 합성 중 어둡게"> |
| **보관함** | <img src="app/screenshots/library-light.png" width="240" alt="보관함 밝게"> | <img src="app/screenshots/library-dark.png" width="240" alt="보관함 어둡게"> |
| **보관함 · 빈 상태** | <img src="app/screenshots/library-empty-light.png" width="240" alt="보관함 · 빈 상태 밝게"> | <img src="app/screenshots/library-empty-dark.png" width="240" alt="보관함 · 빈 상태 어둡게"> |
| **새 대본** | <img src="app/screenshots/add-script-light.png" width="240" alt="새 대본 밝게"> | <img src="app/screenshots/add-script-dark.png" width="240" alt="새 대본 어둡게"> |
| **새 대본 · 읽을 문장 없음** | <img src="app/screenshots/add-error-light.png" width="240" alt="새 대본 · 읽을 문장 없음 밝게"> | <img src="app/screenshots/add-error-dark.png" width="240" alt="새 대본 · 읽을 문장 없음 어둡게"> |
| **공유 받기** | <img src="app/screenshots/share-light.png" width="240" alt="공유 받기 밝게"> | <img src="app/screenshots/share-dark.png" width="240" alt="공유 받기 어둡게"> |
| **설정** | <img src="app/screenshots/settings-light.png" width="240" alt="설정 밝게"> | <img src="app/screenshots/settings-dark.png" width="240" alt="설정 어둡게"> |
| **목소리 없음** | <img src="app/screenshots/no-voice-light.png" width="240" alt="목소리 없음 밝게"> | <img src="app/screenshots/no-voice-dark.png" width="240" alt="목소리 없음 어둡게"> |
| **컴포넌트** | <img src="app/screenshots/component-light.png" width="240" alt="컴포넌트 밝게"> | <img src="app/screenshots/component-dark.png" width="240" alt="컴포넌트 어둡게"> |
| **색 토큰** | <img src="app/screenshots/color-light.png" width="240" alt="색 토큰 밝게"> | <img src="app/screenshots/color-dark.png" width="240" alt="색 토큰 어둡게"> |
| **글자 눈금** | <img src="app/screenshots/type-light.png" width="240" alt="글자 눈금 밝게"> | <img src="app/screenshots/type-dark.png" width="240" alt="글자 눈금 어둡게"> |

<!-- SCREENS:END -->

이 블록은 `./gradlew updateReadme` 가 다시 씁니다. 스크린샷은 통과한 테스트의 산출물이라,
여기 실린 화면은 정의상 깨지지 않은 화면입니다. 마커 밖의 글은 손으로 씁니다.

---

## 4. 만들고 돌리기

### 처음 한 번

```bash
brew install openjdk@21                      # cask 가 아니라 formula — sudo 를 묻지 않는다
brew install --cask android-commandlinetools

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.2" "build-tools;37.0.0"
```

`JAVA_HOME` 은 셸 프로필에 넣어 두는 편이 낫습니다. Gradle 은 래퍼로 받으므로 따로 깔지 않습니다.

### 만들고 설치

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Play Store 를 거치지 않고 APK 를 직접 설치합니다.

### 검사와 스크린샷

```bash
./gradlew check                 # 유닛 테스트 + 스크린샷 회귀 + Android Lint
./gradlew recordRoborazziDebug  # 기준 이미지 다시 찍기 (화면을 의도적으로 바꿨을 때)
./gradlew updateReadme          # 찍혀 있는 기준으로 위 화면 표 갱신 (record 와 함께 돌릴 것)
```

스크린샷은 Robolectric 이 **JVM 에서** 렌더하므로 기기도 에뮬레이터도 필요 없습니다.
찍기 전에 내용이 실제로 그려졌는지 단언하기 때문에, 깨진 화면이 기준 이미지로 남을 수 없습니다.

`updateReadme` 와 `check` 는 **따로 돌려야 합니다.** 한 번에 부르면 유닛 테스트가 한 번만
도는데 그때 검증 모드로 돌아, 방금 다시 찍을 화면을 옛 기준과 견주다 실패합니다.

`updateReadme` 는 찍는 일을 하지 않습니다. 화면을 바꿨으면 `recordRoborazziDebug` 를 먼저
돌리세요 — 두 일을 한 태스크에 묶었더니 Gradle 이 기준 이미지를 오래된 산출물로 보고
지우는데 정작 다시 찍지는 않는 일이 있었습니다.

### 걸려 넘어질 자리

- **JDK 25 로는 안 됩니다.** AGP 가 아직 받지 않아 JDK 21 을 씁니다
- **AGP 9 부터 Kotlin 지원이 내장**이라 `kotlin-android` 플러그인을 적용하면 빌드가 거부됩니다
- 플랫폼 이름이 마이너 버전까지 씁니다 — `platforms;android-37`(없음)이 아니라
  **`android-37.2`**. 빌드 파일에서는 `compileSdk = 37` 과 `compileSdkMinor = 2` 로 나눠 적습니다
- 지금 androidx 스택이 **compileSdk 37 이상**을 요구합니다. 36 으로는 15개 의존성이 거부합니다

---

## 5. 설계 문서

| 문서 | 내용 |
| --- | --- |
| [설계 스펙](docs/superpowers/specs/2026-09-10-script-player-design.md) | 아키텍처, 모듈 사양, 결정 근거, 테스트 전략 |
| [로드맵](ROADMAP.md) | 8 단계 구현 순서와 단계별 완료 조건 |

스펙의 **결정 기록**에는 왜 웹이 아닌지, 왜 미리 파일로 합성하는지, 왜 TypeScript 가
아닌지가 근거와 함께 남아 있습니다. 웹으로 시작했다가 접은 과정도 적혀 있습니다.

---

## 6. 스택

Kotlin · Jetpack Compose · Media3 · Room · Roborazzi · [Pretendard](licenses/)

안드로이드 전용입니다. 화면을 끈 백그라운드 재생과 에어팟 버튼, 그리고 음성을 파일로
합성하는 것(`TextToSpeech.synthesizeToFile`) 이 전부 플랫폼 API 라, 웹이나 크로스플랫폼
래퍼로는 타협 없이 만들 수 없었습니다. 자세한 근거는 스펙에 있습니다.
