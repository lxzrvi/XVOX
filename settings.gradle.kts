pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        // Material 3 Expressive engine (JustForPixel ExpressiveLab) and its smooth-corner
        // geometry primitive are published through JitPack.
        maven("https://jitpack.io")
    }
}

rootProject.name = "XVOX"

include(":app")
