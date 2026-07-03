package dev.defdo.selfcare.config

import dev.defdo.selfcare.BuildConfig

/**
 * Build-time / runtime configuration for the SelfCare app shell.
 *
 * Important: [clientId] is the ONLY brand/app authority the app asserts, and it
 * is asserted to defdo_auth during the OAuth flow — not to the product backend.
 * The app never sends brand_code, brand_key, app_key, theme_code, or tenant as
 * authority to /mobile/bootstrap or /mobile/theme. The backend derives that
 * context from the OAuth client + the AccessContext it returns.
 *
 * No production credentials are hardcoded here. Values come from Gradle
 * properties / environment at build time, with safe dev defaults.
 */
data class AppConfig(
    val issuer: String,
    val discoveryUrl: String,
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>,
    val bootstrapEndpoint: String,
    val themeEndpoint: String,
    val environment: String
) {
    val isConfigured: Boolean
        get() = issuer.isNotBlank() && clientId.isNotBlank()

    /**
     * Helper used by tests and manifest sanity checks to reconstruct the
     * redirect URI from its components. This keeps the manifest placeholders
     * and runtime config consistent.
     */
    fun redirectUri(scheme: String, host: String, path: String): String {
        val normalizedPath = path.trimStart('/')
        return "$scheme://$host/$normalizedPath"
    }

    /**
     * True when the configured redirect URI uses a custom scheme. Verified App
     * Links / HTTPS are preferred for production.
     */
    val usesCustomScheme: Boolean
        get() = !redirectUri.startsWith("https://")

    companion object {
        /**
         * Resolves config from generated BuildConfig fields. This is what the
         * running Android app uses so configuration is baked at build time and
         * does not depend on runtime shell environment variables.
         */
        fun fromBuildConfig(): AppConfig {
            val issuer = BuildConfig.DEFDO_DEV_ISSUER
            val discoveryUrl = BuildConfig.DEFDO_DEV_DISCOVERY_URL
            return AppConfig(
                issuer = issuer,
                discoveryUrl = discoveryUrl,
                clientId = BuildConfig.DEFDO_DEV_CLIENT_ID,
                redirectUri = BuildConfig.DEFDO_DEV_REDIRECT_URI,
                scopes = BuildConfig.DEFDO_DEV_SCOPES.split(" ").filter { it.isNotBlank() },
                bootstrapEndpoint = "${BuildConfig.DEFDO_BACKEND_BASE_URL}/mobile/bootstrap",
                themeEndpoint = "${BuildConfig.DEFDO_BACKEND_BASE_URL}/mobile/theme",
                environment = BuildConfig.DEFDO_ENVIRONMENT
            )
        }

        /**
         * Resolves config from environment variables (dev harness / unit-test
         * style) with non-secret defaults. Production builds inject these
         * through the brand manifest / build config, never as hardcoded credentials.
         */
        fun fromEnvironment(env: (String) -> String? = { System.getenv(it) }): AppConfig {
            val issuer = env("DEFDO_DEV_ISSUER").orEmpty()
            val discoveryUrl = env("DEFDO_DEV_DISCOVERY_URL")
                ?: if (issuer.isNotBlank()) "$issuer/.well-known/openid-configuration" else ""
            val clientId = env("DEFDO_DEV_CLIENT_ID") ?: "defdo-telecom-mobile-dev"
            val redirectUri = env("DEFDO_DEV_REDIRECT_URI")
                ?: "https://login.defdo-telecom.example/mobile/oauth/callback"
            val scopes = (env("DEFDO_DEV_SCOPES") ?: "openid profile offline_access")
                .split(" ")
                .filter { it.isNotBlank() }
            // Backend base for the product BFF. The bootstrap response also
            // returns a theme endpoint hint; that hint wins at runtime.
            val backendBase = env("DEFDO_BACKEND_BASE_URL") ?: "https://api.defdo.example"

            return AppConfig(
                issuer = issuer,
                discoveryUrl = discoveryUrl,
                clientId = clientId,
                redirectUri = redirectUri,
                scopes = scopes,
                bootstrapEndpoint = "$backendBase/mobile/bootstrap",
                themeEndpoint = "$backendBase/mobile/theme",
                environment = env("DEFDO_ENVIRONMENT") ?: "dev"
            )
        }
    }
}
