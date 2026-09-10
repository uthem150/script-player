plugins {
    // AGP 9 부터 Kotlin 지원이 내장이라 kotlin-android 플러그인을 적용하지 않는다.
    // 적용하면 빌드가 거부된다. https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
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
        versionCode = 1
        versionName = "0.1.0"
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

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
