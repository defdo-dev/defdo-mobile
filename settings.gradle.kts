pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.0.21"
        kotlin("android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
        id("com.android.application") version "8.7.3"
        id("com.android.library") version "8.7.3"
    }
}

// Auto-provision JDK toolchains (jvmToolchain(17)) on machines/CI without a
// matching local JDK.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "defdo-mobile"

include(":android:modules:auth")
include(":android:modules:auth-android")
include(":android:modules:theme")
include(":android:apps:dev-auth-harness")
include(":android:apps:defdo-selfcare")
