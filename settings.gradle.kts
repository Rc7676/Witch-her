pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hexfall"
include(":core")

// The game rules in :core are pure Kotlin and can be built/tested on any JVM
// without the Android SDK: gradle -Phexfall.coreOnly=true :core:test
if (!providers.gradleProperty("hexfall.coreOnly").isPresent) {
    include(":app")
}
