plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nidoham.charlink"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.nidoham.charlink"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    // ---------------- AndroidX Core ----------------
    implementation(libs.androidx.core.ktx)      // 🔵 বেসিক Kotlin এক্সটেনশন
    implementation(libs.androidx.appcompat)     // 🔵 পুরনো UI Compatibility
    implementation(libs.material)               // 🔵 Material Components

    // ---------------- Lifecycle + Architecture ----------------
    implementation(libs.androidx.lifecycle.runtime.ktx)   // 🔵 Lifecycle coroutine support
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 🔵 ViewModel for Compose
    implementation(libs.androidx.lifecycle.runtime.compose)   // 🔵 Compose lifecycle aware

    // ---------------- Activity / Navigation ----------------
    implementation(libs.androidx.activity)             // 🔵 Activity KTX
    implementation(libs.androidx.activity.compose)     // 🔵 Compose Activity integration
    implementation(libs.androidx.navigation.compose)   // 🔵 Compose Navigation

    // ---------------- Jetpack Compose ----------------
    implementation(platform(libs.androidx.compose.bom)) // 🔵 Compose BOM version sync
    implementation(libs.androidx.compose.ui)            // 🔵 Core UI
    implementation(libs.androidx.compose.ui.graphics)   // 🔵 Graphics
    implementation(libs.androidx.compose.material3)     // 🔵 Material 3 UI
    implementation(libs.androidx.compose.ui.tooling.preview) // 🔵 Preview support
    implementation(libs.androidx.compose.material.icons.extended) // 🔵 Extra icons

    debugImplementation(libs.androidx.compose.ui.tooling)       // 🔵 Debug preview tools
    debugImplementation(libs.androidx.compose.ui.test.manifest) // 🔵 Test manifest

    // ---------------- Image Loading ----------------
    implementation(libs.coil.compose) // 🔵 Coil দিয়ে Compose-এ ইমেজ লোড

    // ---------------- Networking ----------------
    implementation(libs.okhttp) // 🔵 OkHttp client
    implementation(libs.gson)   // 🔵 JSON পার্সিং

    // ---------------- Coroutines ----------------
    implementation(libs.kotlinx.coroutines.play.services) // 🔵 Play-services coroutine support

    // ---------------- Firebase ----------------
    implementation(platform(libs.firebase.bom)) // 🔵 Firebase BOM
    implementation(libs.firebase.analytics)     // 🔵 Firebase Analytics
    implementation(libs.firebase.auth)          // 🔵 Auth
    implementation(libs.firebase.database)      // 🔵 RTDB
    implementation(libs.firebase.firestore)     // 🔵 Firestore
    implementation(libs.firebase.ai)            // 🔵 Firebase GenAI

    // ---------------- Google Sign-In ----------------
    implementation(libs.play.services.auth) // 🔵 Google Login

    // ---------------- Testing ----------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}