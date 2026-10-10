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

    // Keep the universal APK download compact; native libraries are extracted on install.
    packaging { jniLibs.useLegacyPackaging = true }

    buildFeatures {
        compose = true
    }
}

dependencies {
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
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    // Genuine Chris Banes Haze backdrop blur. Haze 1.6.5 is the newest line whose core and
    // materials artifacts are both published together; the requested haze-blur:1.6.5 module
    // does not exist on Maven Central (that module starts at Haze 2.x).
    implementation("dev.chrisbanes.haze:haze:1.6.5")
    implementation("dev.chrisbanes.haze:haze-materials:1.6.5")
    implementation("net.jthink:jaudiotagger:3.0.1")
}
