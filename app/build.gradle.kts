plugins {
    // AGP 9 부터 Kotlin 지원이 내장이라 kotlin-android 플러그인을 적용하지 않는다.
    // 적용하면 빌드가 거부된다. https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
}
