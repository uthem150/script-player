plugins {
    // AGP 9 부터 Kotlin 지원이 내장이라 kotlin-android 플러그인을 적용하지 않는다.
    // 적용하면 빌드가 거부된다. https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    // 스키마를 저장소에 남긴다. 나중에 이주를 쓰게 되면 이 파일이 유일한 근거가 된다.
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "dev.uthem.scriptplayer"

    // 플랫폼 이름이 마이너 버전까지 쓰는 방식으로 바뀌었다 (platforms;android-37.2).
    // 지금 androidx 스택이 37 이상으로 컴파일하라고 요구한다.
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "dev.uthem.scriptplayer"
        minSdk = 26

        // 컴파일은 37 로 하되 런타임 동작 옵트인은 36 에 둔다 —
        // 새 제약을 한꺼번에 떠안지 않고, 재생·포그라운드 서비스가 선 뒤에 올린다.
        targetSdk = 36
        versionCode = 4
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric 이 리소스와 테마를 읽어야 화면을 렌더할 수 있다.
            isIncludeAndroidResources = true
        }
    }
}

roborazzi {
    // 기준 이미지는 저장소에 있어야 한다 — build/ 아래에 두면 회귀 검증이 성립하지 않는다.
    outputDir.set(file("screenshots"))
}

/*
 * `./gradlew check` 한 줄로 스크린샷 회귀까지 잡히게 한다.
 *
 * 검증을 따로 기억해서 돌려야 하면 언젠가 안 돌린다. verifyRoborazziDebug 가 태스크 그래프에
 * 있으면 Roborazzi 가 유닛 테스트를 검증 모드로 돌리므로 테스트가 두 번 돌지는 않는다.
 */
tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

/*
 * README 의 화면 블록을 다시 쓴다.
 *
 * 스크린샷을 따로 복사하지 않고 **기준 이미지를 그대로 가리킨다.** 사본을 두면 기준이
 * 갱신됐는데 README 만 옛 사진인 상태가 생긴다. 같은 파일을 보면 그 상태가 아예 없다.
 *
 * 여기 실리는 사진은 통과한 테스트의 산출물이라, 정의상 깨지지 않은 화면이다.
 * 마커 밖의 글은 사람이 쓴다 — 생성기가 설명을 덮지 않게 하는 경계다.
 */
private val readmeScreens = listOf(
    Triple("재생기", "player-light.png", "player-dark.png"),
    Triple("재생기 · 정지와 합성 중", "player-paused-light.png", "player-synthesizing-dark.png"),
    Triple("보관함", "library-light.png", "library-dark.png"),
    Triple("보관함 · 빈 상태", "library-empty-light.png", "library-empty-dark.png"),
    Triple("새 대본", "add-script-light.png", "add-script-dark.png"),
    Triple("새 대본 · 읽을 문장 없음", "add-error-light.png", "add-error-dark.png"),
    Triple("공유 받기", "share-light.png", "share-dark.png"),
    Triple("설정", "settings-light.png", "settings-dark.png"),
    Triple("목소리 없음", "no-voice-light.png", "no-voice-dark.png"),
    Triple("컴포넌트", "component-light.png", "component-dark.png"),
    Triple("색 토큰", "color-light.png", "color-dark.png"),
    Triple("글자 눈금", "type-light.png", "type-dark.png"),
)

/*
 * 찍는 일과 README 를 쓰는 일을 나눈다.
 *
 * 처음에 `recordRoborazziDebug` 에 의존하게 뒀더니, Gradle 이 그 태스크의 산출물을
 * 오래된 것으로 보고 지웠는데 태스크는 UP-TO-DATE 라 다시 쓰지 않아, 있던 기준 이미지가
 * 사라진 채로 README 를 쓰려 했다. 한 태스크가 한 가지만 하게 하면 이 얽힘이 없다.
 *
 * 함께 돌릴 때: `./gradlew recordRoborazziDebug updateReadme`
 */
tasks.register("updateReadme") {
    description = "이미 찍혀 있는 기준 이미지로 README 의 화면 블록을 갱신한다"

    // 설정 캐시를 위해 Project 를 doLast 안에서 건드리지 않는다 — File 만 미리 붙잡는다.
    val shotsDir = layout.projectDirectory.dir("screenshots").asFile
    val readme = rootProject.layout.projectDirectory.file("README.md").asFile
    val relativePrefix = "app/screenshots"
    val screens = readmeScreens

    doLast {
        val missing = screens
            .flatMap { listOf(it.second, it.third) }
            .filterNot { File(shotsDir, it).isFile }
        require(missing.isEmpty()) {
            "기준 이미지가 없습니다: $missing — recordRoborazziDebug 가 이 이름으로 찍는지 확인하세요"
        }

        val table = buildString {
            appendLine("| | 밝게 | 어둡게 |")
            appendLine("| --- | --- | --- |")
            screens.forEach { (caption, light, dark) ->
                appendLine(
                    "| **$caption** " +
                        "| <img src=\"$relativePrefix/$light\" width=\"240\" alt=\"$caption 밝게\"> " +
                        "| <img src=\"$relativePrefix/$dark\" width=\"240\" alt=\"$caption 어둡게\"> |",
                )
            }
        }.trimEnd()

        val begin = "<!-- SCREENS:BEGIN -->"
        val end = "<!-- SCREENS:END -->"
        val text = readme.readText()
        val from = text.indexOf(begin)
        val to = text.indexOf(end)
        require(from >= 0 && to > from) { "README 에 $begin / $end 마커가 없습니다" }

        readme.writeText(
            text.substring(0, from + begin.length) +
                "\n\n" + table + "\n\n" +
                text.substring(to),
        )
        logger.lifecycle("README 화면 블록 갱신: ${screens.size}줄")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
