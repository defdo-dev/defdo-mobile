package dev.defdo.mobile.auth

sealed class LoginResult {
    data class PendingBrowser(val authorizationUrl: String) : LoginResult()
    data class Authenticated(val session: AuthSession) : LoginResult()
    data class Failed(val error: AuthError) : LoginResult()
    object LoggedOut : LoginResult()
}
