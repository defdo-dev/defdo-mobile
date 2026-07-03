package dev.defdo.mobile.auth

sealed class AuthError(message: String? = null) : Exception(message) {
    class InvalidDiscovery(reason: String) : AuthError(reason)
    class InvalidCallback(reason: String) : AuthError(reason)
    class OAuthError(val code: String) : AuthError(code)
    object RequiresLogin : AuthError()
    object Retryable : AuthError()

    companion object {
        fun normalize(error: String): AuthError = AuthErrorNormalizer.normalize(error)
    }
}
