package dev.defdo.mobile.auth

object AuthErrorNormalizer {
    private val mapping: Map<String, AuthError> = mapOf(
        "invalid_request" to AuthError.OAuthError("invalid_request"),
        "invalid_grant" to AuthError.RequiresLogin,
        "temporarily_unavailable" to AuthError.Retryable,
        "access_denied" to AuthError.OAuthError("user_cancelled")
    )

    fun normalize(error: String): AuthError = mapping[error] ?: AuthError.OAuthError(error)
}
