package dev.defdo.mobile.auth

fun runAuthErrorNormalizerTest() {
    val normalizationFixture = AuthTestFixtures.loadJson("auth_error_normalization.json")

    for ((error, expected) in normalizationFixture) {
        val normalized = AuthErrorNormalizer.normalize(error)
        when (expected) {
            "invalid_request" -> check(normalized is AuthError.OAuthError && normalized.code == "invalid_request")
            "requires_login" -> check(normalized is AuthError.RequiresLogin)
            "retryable" -> check(normalized is AuthError.Retryable)
            "user_cancelled" -> check(normalized is AuthError.OAuthError && normalized.code == "user_cancelled")
        }
    }

    val unknown = AuthErrorNormalizer.normalize("unknown_error")
    check(unknown is AuthError.OAuthError) { "unknown error should normalize to OAuthError" }
    check((unknown as AuthError.OAuthError).code == "unknown_error")
}
