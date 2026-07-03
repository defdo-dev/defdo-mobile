package dev.defdo.selfcare.session

import dev.defdo.mobile.auth.DefdoAuthMobileClient

/**
 * Real SessionProvider backed by the auth library. Tokens are read only from
 * the auth client's SecureTokenStore (Android Keystore / EncryptedSharedPrefs).
 * No tokens are ever read from or written to plain SharedPreferences here.
 */
class AuthClientSessionProvider(
    private val client: DefdoAuthMobileClient
) : SessionProvider {

    override fun currentAccessToken(): String? =
        client.currentSession()?.accessToken

    override fun clear() {
        val session = client.currentSession() ?: return
        // revoke() also clears local secure storage.
        client.revoke(session)
    }
}
