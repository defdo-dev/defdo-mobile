package dev.defdo.selfcare

import dev.defdo.selfcare.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Build-time sanity checks that mirror manifest / packaging expectations.
 * These catch accidental changes to applicationId, redirect URI shape, or the
 * App Link host before the APK is installed on a device.
 */
class ManifestSanityTest {

    @Test
    fun applicationIdMatchesSelfCarePackage() {
        assertEquals("dev.defdo.selfcare", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun redirectUriIsConfigured() {
        assertTrue(BuildConfig.DEFDO_DEV_REDIRECT_URI.isNotBlank(), "redirect URI must be configured")
    }

    @Test
    fun defaultRedirectUsesHttps() {
        // The default / placeholder redirect must be HTTPS so the production
        // path is App Links. Custom schemes are dev-only and must be opt-in.
        assertTrue(
            BuildConfig.DEFDO_DEV_REDIRECT_URI.startsWith("https://"),
            "default redirect must use https for App Links: ${BuildConfig.DEFDO_DEV_REDIRECT_URI}"
        )
    }

    @Test
    fun bootstrapAndThemeEndpointsUseHttps() {
        assertTrue(BuildConfig.DEFDO_BACKEND_BASE_URL.startsWith("https://"))
    }
}
