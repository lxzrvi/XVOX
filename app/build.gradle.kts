plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.xvox.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xvox.music"

        // Android 13 remains the supported product baseline.
        minSdk = 33
        targetSdk = 37

        // v1.02.3: post-release media fixes plus GIF crop, playback, and Settings refinements.
        versionCode = 10203
        versionName = "1.02.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Keep APK downloads compact. minSdk 33 permits compressed legacy DEX inside distributable
    // APKs without changing App Bundle delivery behaviour.
    packaging {
        jniLibs.useLegacyPackaging = true
        dex.useLegacyPackaging = true
    }

    // Every release delivers the three requested device ABIs plus a universal fallback. The CI
    // workflow reads output metadata rather than relying on AGP's generated source filenames.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        // CI now publishes the release variant. Keep debug similarly optimized for local parity;
        // R8 only removes unreachable bytecode/resources and rewrites the remaining DEX compactly.
        getByName("debug") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main") {
            // The supplied artwork stays at res/default_cover as the canonical source requested
            // by the product. AGP 9 source directories no longer expose file filters, so compile
            // the byte-identical drawable-nodpi copy from a dedicated valid resource root instead
            // of ever asking aapt to interpret `default_cover` as a resource type.
            res.directories.clear()
            res.directories.add("src/main/android-res")
        }
    }

    buildFeatures {
        compose = true
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
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
    implementation("net.jthink:jaudiotagger:3.0.1")
    // Pure-Java Ogg/Opus packet writer used for standards-compliant OpusTags title/artwork edits.
    implementation("org.gagravarr:vorbis-java-core:0.8")
}
