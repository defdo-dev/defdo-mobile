package dev.defdo.selfcare.session

/**
 * Narrow session port over the auth library, so coordinators stay unit-testable
 * without the full DefdoAuthMobileClient + platform adapters.
 *
 * The real implementation (AuthClientSessionProvider) wraps
 * dev.defdo.mobile.auth.DefdoAuthMobileClient and reads tokens only from secure
 * storage — never from plain preferences.
 */
interface SessionProvider {
    /** Current access token from secure storage, or null if signed out. */
    fun currentAccessToken(): String?

    /** Clear local session (revoke/forget tokens in secure storage). */
    fun clear()
}

/**
 * Owns the signed-in / signed-out determination and session invalidation.
 * Token persistence itself lives in the auth library's SecureTokenStore backed
 * by Android Keystore / EncryptedSharedPreferences.
 */
class AuthSessionCoordinator(
    private val provider: SessionProvider
) {
    fun hasSession(): Boolean = !provider.currentAccessToken().isNullOrBlank()

    fun accessToken(): String? = provider.currentAccessToken()

    /** Invalidate session on 401 from bootstrap or theme. */
    fun invalidate() = provider.clear()
}
