plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.xvox.music"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xvox.music"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Keep the universal APK download compact. Native libraries are extracted on install;
    // the bundled ARM64 ONNX libraries themselves use 16 KB-compatible ELF alignment.
    packaging { jniLibs.useLegacyPackaging = true }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.2")
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("net.jthink:jaudiotagger:3.0.1")
}
