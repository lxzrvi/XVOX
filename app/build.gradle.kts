plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(
        "androidx.compose:compose-bom:2025.08.00"
    )

    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")

    implementation("androidx.core:core-splashscreen:1.0.1")
}
