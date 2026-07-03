plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "dev.defdo.mobile.harness"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.defdo.mobile.harness"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        manifestPlaceholders["defdoAuthRedirectScheme"] =
            project.findProperty("defdo.redirect.scheme") as String? ?: "https"
        manifestPlaceholders["defdoAuthRedirectHost"] =
            project.findProperty("defdo.redirect.host") as String? ?: "app.defdo.example"
        manifestPlaceholders["defdoAuthRedirectPathPrefix"] =
            project.findProperty("defdo.redirect.path") as String? ?: "/oauth/callback"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":android:modules:auth"))
    implementation(project(":android:modules:auth-android"))
    implementation("androidx.appcompat:appcompat:1.6.1")
}
