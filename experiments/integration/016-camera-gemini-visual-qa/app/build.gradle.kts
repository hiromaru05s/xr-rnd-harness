plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.camerageminiqa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.camerageminiqa"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // AIグラス必須ライブラリ
    implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")
    implementation("androidx.xr.projected:projected:1.0.0-alpha05")
    implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")

    // Compose基盤
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-core")

    // Kotlin/Android基盤
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // CameraX（グラスカメラアクセス）
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")

    // Gemini Live API（Firebase AI Logic）
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")

    // テスト
    testImplementation("junit:junit:4.13.2")
}
