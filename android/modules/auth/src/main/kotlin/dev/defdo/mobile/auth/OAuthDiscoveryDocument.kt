package dev.defdo.mobile.auth

data class OAuthDiscoveryDocument(
    val issuer: String?,
    val authorizationEndpoint: String?,
    val tokenEndpoint: String?,
    val revocationEndpoint: String? = null,
    val userinfoEndpoint: String? = null,
    val jwksUri: String? = null,
    val codeChallengeMethodsSupported: List<String>? = null,
    val scopesSupported: List<String>? = null,
    val grantTypesSupported: List<String>? = null,
    val responseTypesSupported: List<String>? = null,
    val tokenEndpointAuthMethodsSupported: List<String>? = null
) {
    fun validate(): AuthError? = validate(null)

    fun validate(expectedIssuer: String?): AuthError? {
        if (issuer.isNullOrBlank()) return AuthError.InvalidDiscovery("missing issuer")
        if (authorizationEndpoint.isNullOrBlank()) return AuthError.InvalidDiscovery("missing authorization_endpoint")
        if (tokenEndpoint.isNullOrBlank()) return AuthError.InvalidDiscovery("missing token_endpoint")

        if (expectedIssuer != null && issuer != expectedIssuer) {
            return AuthError.InvalidDiscovery("issuer mismatch: expected $expectedIssuer, got $issuer")
        }

        for ((name, url) in listOf(
            "authorization_endpoint" to authorizationEndpoint,
            "token_endpoint" to tokenEndpoint,
            "revocation_endpoint" to revocationEndpoint
        )) {
            if (url != null && !url.startsWith("https://")) {
                return AuthError.InvalidDiscovery("$name must use HTTPS")
            }
        }

        if (expectedIssuer != null) {
            val issuerHost = try {
                java.net.URI(expectedIssuer).host
            } catch (_: Exception) { null }
            for ((name, url) in listOf(
                "authorization_endpoint" to authorizationEndpoint,
                "token_endpoint" to tokenEndpoint,
                "revocation_endpoint" to revocationEndpoint
            )) {
                if (url != null && issuerHost != null) {
                    val endpointHost = try {
                        java.net.URI(url).host
                    } catch (_: Exception) { null }
                    if (endpointHost != null && endpointHost != issuerHost) {
                        return AuthError.InvalidDiscovery("$name host ($endpointHost) does not match issuer host ($issuerHost)")
                    }
                }
            }
        }

        if (codeChallengeMethodsSupported != null && "S256" !in codeChallengeMethodsSupported) {
            return AuthError.InvalidDiscovery("code_challenge_methods_supported does not include S256")
        }

        if (responseTypesSupported != null && "code" !in responseTypesSupported) {
            return AuthError.InvalidDiscovery("response_types_supported does not include code")
        }

        if (grantTypesSupported != null && "authorization_code" !in grantTypesSupported) {
            return AuthError.InvalidDiscovery("grant_types_supported does not include authorization_code")
        }

        return null
    }
}
