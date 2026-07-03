plugins {
    id("com.android.library")
    kotlin("android")
}

group = "dev.defdo.mobile"
version = "0.1.0"

android {
    namespace = "dev.defdo.mobile.auth.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    implementation("androidx.browser:browser:1.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    testImplementation(kotlin("test"))
}
