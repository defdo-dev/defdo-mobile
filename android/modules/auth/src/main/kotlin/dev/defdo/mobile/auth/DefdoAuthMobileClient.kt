package dev.defdo.mobile.auth

interface DefdoAuthMobileClient {
    fun startLogin(request: LoginRequest): LoginResult.PendingBrowser
    fun handleCallback(callbackUrl: String, expectedState: String, expectedRedirectUri: String): LoginResult
    fun currentSession(): AuthSession?
    fun refresh(session: AuthSession): LoginResult
    fun revoke(session: AuthSession): LoginResult.LoggedOut
}
