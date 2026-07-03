package dev.defdo.mobile.auth

object AuthorizationRequestBuilder {
    fun parameters(request: LoginRequest): Map<String, String> {
        validate(request)
        val config = request.config
        val params = linkedMapOf(
            "response_type" to "code",
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "scope" to config.scopes.joinToString(" "),
            "state" to request.state,
            "code_challenge" to PKCE.s256Challenge(request.codeVerifier),
            "code_challenge_method" to "S256"
        )
        if (config.useNonce && !request.nonce.isNullOrBlank()) {
            params["nonce"] = request.nonce
        }
        return params
    }

    fun url(authorizationEndpoint: String, request: LoginRequest): String {
        if (authorizationEndpoint.isBlank()) throw IllegalArgumentException("missing authorization_endpoint")
        val query = parameters(request).entries.joinToString("&") { (key, value) ->
            "${rfc3986Encode(key)}=${rfc3986Encode(value)}"
        }
        return "$authorizationEndpoint?$query"
    }

    private fun validate(request: LoginRequest) {
        require(request.config.clientId.isNotBlank()) { "missing client_id" }
        require(request.config.redirectUri.isNotBlank()) { "missing redirect_uri" }
        require(request.config.scopes.isNotEmpty()) { "empty scopes" }
        require(PKCE.isValidVerifier(request.codeVerifier)) { "invalid code_challenge" }
    }

    private fun rfc3986Encode(value: String): String {
        val sb = StringBuilder()
        for (c in value) {
            if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '.' || c == '_' || c == '~') {
                sb.append(c)
            } else {
                sb.append(String.format("%%%02X", c.code))
            }
        }
        return sb.toString()
    }
}
