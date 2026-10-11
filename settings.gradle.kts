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
        // Official PrismalAGSL publication repository.
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "XVOX"

include(":app")
