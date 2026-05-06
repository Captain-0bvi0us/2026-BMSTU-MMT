plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bmstu.iu5.gazetracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bmstu.iu5.gazetracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-week1"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Compose Compiler, совместимый с Kotlin 1.9.22
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // MediaPipe-модель .task — это уже сжатый бинарник, нельзя дополнительно жать в APK
    androidResources {
        noCompress += listOf("task", "tflite")
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
            )
        }
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")
}

dependencies {
    // ---------- AndroidX core / lifecycle ----------
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // ---------- Jetpack Compose ----------
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---------- CameraX ----------
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ---------- MediaPipe Tasks Vision (Face Landmarker) ----------
    // 0.10.14 и ниже: нативные библиотеки без выравнивания под 16 KB страницы —
    // на Android 15+ с page size 16 KB приложение падает при загрузке JNI / Studio
    // показывает предупреждение про libimage_processing_util_jni.so.
    // См. https://github.com/google-ai-edge/mediapipe/issues/6028 — фикс с 0.10.26.
    implementation("com.google.mediapipe:tasks-vision:0.10.26")

    // ---------- Tests ----------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
