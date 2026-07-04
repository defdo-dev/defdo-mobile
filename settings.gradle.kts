pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "2.2.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
        id("com.android.application") version "9.0.1"
        id("com.android.library") version "9.0.1"
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
include(":android:modules:selfcare-core")
include(":android:modules:theme")
include(":android:apps:dev-auth-harness")
include(":android:apps:defdo-selfcare")
