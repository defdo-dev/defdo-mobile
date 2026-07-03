package dev.defdo.selfcare.config

import dev.defdo.selfcare.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {

    @Test
    fun fromBuildConfigMatchesGeneratedDefaults() {
        val config = AppConfig.fromBuildConfig()

        assertEquals(BuildConfig.DEFDO_DEV_ISSUER, config.issuer)
        assertEquals(BuildConfig.DEFDO_DEV_CLIENT_ID, config.clientId)
        assertEquals(BuildConfig.DEFDO_DEV_REDIRECT_URI, config.redirectUri)
        assertEquals(BuildConfig.DEFDO_DEV_SCOPES.split(" ").filter { it.isNotBlank() }, config.scopes)
        assertEquals("${BuildConfig.DEFDO_BACKEND_BASE_URL}/mobile/bootstrap", config.bootstrapEndpoint)
        assertEquals("${BuildConfig.DEFDO_BACKEND_BASE_URL}/mobile/theme", config.themeEndpoint)
        assertEquals(BuildConfig.DEFDO_ENVIRONMENT, config.environment)
    }

    @Test
    fun fromEnvironmentUsesDefaultsWhenUnset() {
        val config = AppConfig.fromEnvironment { null }

        assertEquals("", config.issuer)
        assertEquals("", config.discoveryUrl)
        assertEquals("defdo-telecom-mobile-dev", config.clientId)
        assertEquals("https://login.defdo-telecom.example/mobile/oauth/callback", config.redirectUri)
        assertEquals(listOf("openid", "profile", "offline_access"), config.scopes)
        assertEquals("https://api.defdo.example/mobile/bootstrap", config.bootstrapEndpoint)
        assertEquals("https://api.defdo.example/mobile/theme", config.themeEndpoint)
        assertEquals("dev", config.environment)
        assertFalse(config.isConfigured, "issuer is blank so config is not configured")
    }

    @Test
    fun fromEnvironmentBuildsDiscoveryUrlFromIssuer() {
        val config = AppConfig.fromEnvironment {
            when (it) {
                "DEFDO_DEV_ISSUER" -> "https://auth.example"
                else -> null
            }
        }

        assertEquals("https://auth.example/.well-known/openid-configuration", config.discoveryUrl)
        assertTrue(config.isConfigured)
    }

    @Test
    fun fromEnvironmentCanOverrideRedirectUri() {
        val config = AppConfig.fromEnvironment {
            when (it) {
                "DEFDO_DEV_REDIRECT_URI" -> "https://login.example/app/callback"
                else -> null
            }
        }

        assertEquals("https://login.example/app/callback", config.redirectUri)
        assertFalse(config.usesCustomScheme)
    }

    @Test
    fun customSchemeRedirectIsDetected() {
        val config = AppConfig.fromEnvironment {
            when (it) {
                "DEFDO_DEV_REDIRECT_URI" -> "defdo.selfcare.dev://oauth.callback/"
                else -> null
            }
        }

        assertTrue(config.usesCustomScheme)
    }

    @Test
    fun redirectUriHelperReconstructsHttpsCallback() {
        val config = AppConfig.fromEnvironment { null }
        val uri = config.redirectUri("https", "login.example", "/mobile/oauth/callback")

        assertEquals("https://login.example/mobile/oauth/callback", uri)
    }

    @Test
    fun redirectUriHelperNormalizesLeadingSlash() {
        val config = AppConfig.fromEnvironment { null }
        val uri = config.redirectUri("defdo.selfcare.dev", "oauth.callback", "complete")

        assertEquals("defdo.selfcare.dev://oauth.callback/complete", uri)
    }

    @Test
    fun appDoesNotSendAuthorityParamsInEndpoints() {
        // Sanity check: default endpoint URLs must not include authority terms.
        val config = AppConfig.fromEnvironment { null }
        val endpoints = listOf(config.bootstrapEndpoint, config.themeEndpoint)
        val forbidden = listOf("brand_code", "brand_key", "app_key", "theme_code", "tenant")

        for (endpoint in endpoints) {
            val lower = endpoint.lowercase()
            for (term in forbidden) {
                assertFalse(lower.contains(term), "endpoint must not carry '$term' as authority: $endpoint")
            }
        }
    }
}
