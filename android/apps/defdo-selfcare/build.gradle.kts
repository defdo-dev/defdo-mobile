plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.defdo.selfcare"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.defdo.selfcare"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // App identity (build-time). These are NOT sent to the backend as
        // tenant/brand authority — the backend derives app/brand context from
        // the configured OAuth client_id + AccessContext. See docs/mobile_app_shell.md.
        val defdoRedirectScheme =
            project.findProperty("defdo.redirect.scheme") as String? ?: "https"
        val defdoRedirectHost =
            project.findProperty("defdo.redirect.host") as String?
                ?: "login.defdo-telecom.example"
        val defdoRedirectPath =
            project.findProperty("defdo.redirect.path") as String?
                ?: "/mobile/oauth/callback"

        manifestPlaceholders["defdoAuthRedirectScheme"] = defdoRedirectScheme
        manifestPlaceholders["defdoAuthRedirectHost"] = defdoRedirectHost
        manifestPlaceholders["defdoAuthRedirectPathPrefix"] = defdoRedirectPath

        // Dev OAuth / BFF config. Non-secret defaults only; production builds
        // inject these via Gradle properties / CI secrets. See
        // docs/mobile_device_smoke_runbook.md for registration checklist.
        val devIssuer = propertyOrEnv("defdo.dev.issuer", "DEFDO_DEV_ISSUER") ?: ""
        val devDiscoveryUrl = propertyOrEnv("defdo.dev.discoveryUrl", "DEFDO_DEV_DISCOVERY_URL")
            ?: (devIssuer.takeIf { it.isNotBlank() }?.let { "$it/.well-known/openid-configuration" } ?: "")
        val devClientId = propertyOrEnv("defdo.dev.clientId", "DEFDO_DEV_CLIENT_ID")
            ?: "defdo-telecom-mobile-dev"
        val devRedirectUri = propertyOrEnv("defdo.dev.redirectUri", "DEFDO_DEV_REDIRECT_URI")
            ?: "$defdoRedirectScheme://$defdoRedirectHost${defdoRedirectPath}"
        val devScopes = propertyOrEnv("defdo.dev.scopes", "DEFDO_DEV_SCOPES")
            ?: "openid profile offline_access"
        val backendBase = propertyOrEnv("defdo.backendBaseUrl", "DEFDO_BACKEND_BASE_URL")
            ?: "https://api.defdo.example"
        val environment = propertyOrEnv("defdo.environment", "DEFDO_ENVIRONMENT") ?: "dev"

        buildConfigField("String", "DEFDO_DEV_ISSUER", "\"$devIssuer\"")
        buildConfigField("String", "DEFDO_DEV_DISCOVERY_URL", "\"$devDiscoveryUrl\"")
        buildConfigField("String", "DEFDO_DEV_CLIENT_ID", "\"$devClientId\"")
        buildConfigField("String", "DEFDO_DEV_REDIRECT_URI", "\"$devRedirectUri\"")
        buildConfigField("String", "DEFDO_DEV_SCOPES", "\"$devScopes\"")
        buildConfigField("String", "DEFDO_BACKEND_BASE_URL", "\"$backendBase\"")
        buildConfigField("String", "DEFDO_ENVIRONMENT", "\"$environment\"")
    }

    signingConfigs {
        create("dev") {
            // Dev-only signing. Uses the standard Android debug keystore by
            // default so the APK can be installed on a device without a
            // committed production keystore. Override via Gradle properties
            // or environment for your workstation / CI.
            storeFile = file(
                project.findProperty("defdo.dev.storeFile") as String?
                    ?: System.getenv("DEFDO_DEV_STORE_FILE")
                    ?: "${System.getProperty("user.home")}/.android/debug.keystore"
            )
            storePassword =
                project.findProperty("defdo.dev.storePassword") as String?
                    ?: System.getenv("DEFDO_DEV_STORE_PASSWORD")
                    ?: "android"
            keyAlias =
                project.findProperty("defdo.dev.keyAlias") as String?
                    ?: System.getenv("DEFDO_DEV_KEY_ALIAS")
                    ?: "androiddebugkey"
            keyPassword =
                project.findProperty("defdo.dev.keyPassword") as String?
                    ?: System.getenv("DEFDO_DEV_KEY_PASSWORD")
                    ?: "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("dev")
            // Dev builds surface non-blocking theme diagnostics (403/404).
            buildConfigField("boolean", "DEV_DIAGNOSTICS", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEV_DIAGNOSTICS", "false")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":android:modules:auth"))
    implementation(project(":android:modules:auth-android"))
    implementation(project(":android:modules:theme"))

    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.withType<Test>().configureEach {
    workingDir = rootProject.projectDir
}

fun propertyOrEnv(propertyKey: String, envKey: String): String? {
    return project.findProperty(propertyKey) as String?
        ?: rootProject.findProperty(propertyKey) as String?
        ?: System.getenv(envKey)
}
