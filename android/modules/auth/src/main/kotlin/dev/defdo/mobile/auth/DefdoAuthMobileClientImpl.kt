package dev.defdo.mobile.auth

class DefdoAuthMobileClientImpl(
    private val config: AuthConfig,
    private val discovery: OAuthDiscoveryDocument,
    private val browserAdapter: BrowserAuthAdapter,
    private val tokenStore: TokenStore,
    private val tokenTransport: TokenHttpTransport
) : DefdoAuthMobileClient {

    private val pendingVerifiers = mutableMapOf<String, String>()

    override fun startLogin(request: LoginRequest): LoginResult.PendingBrowser {
        pendingVerifiers[request.state] = request.codeVerifier
        val authorizationUrl = AuthorizationRequestBuilder.url(
            requireNotNull(discovery.authorizationEndpoint) { "missing authorization_endpoint" },
            request
        )
        browserAdapter.openAuthorizationUrl(authorizationUrl)
        return LoginResult.PendingBrowser(authorizationUrl)
    }

    override fun handleCallback(
        callbackUrl: String,
        expectedState: String,
        expectedRedirectUri: String
    ): LoginResult {
        val codeVerifier = pendingVerifiers.remove(expectedState)
            ?: return LoginResult.Failed(AuthError.InvalidCallback("no pending login for state"))

        val code = CallbackValidator.validate(callbackUrl, expectedState, expectedRedirectUri)
            .getOrElse { return LoginResult.Failed(it as AuthError) }

        val tokenEndpoint = requireNotNull(discovery.tokenEndpoint) { "missing token_endpoint" }
        val params = mapOf(
            "grant_type" to "authorization_code",
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "code" to code,
            "code_verifier" to codeVerifier
        )

        return tokenTransport.exchangeCode(tokenEndpoint, params).fold(
            onSuccess = { response -> onTokenResponse(response) },
            onFailure = { error -> LoginResult.Failed(error as AuthError) }
        )
    }

    override fun currentSession(): AuthSession? = tokenStore.read()

    override fun refresh(session: AuthSession): LoginResult {
        val tokenEndpoint = requireNotNull(discovery.tokenEndpoint) { "missing token_endpoint" }
        val params = mapOf(
            "grant_type" to "refresh_token",
            "client_id" to config.clientId,
            "refresh_token" to requireNotNull(session.refreshToken) { "missing refresh_token" }
        )

        return tokenTransport.refreshToken(tokenEndpoint, params).fold(
            onSuccess = { response ->
                if (response.requiresLogin) {
                    tokenStore.clear()
                    return LoginResult.Failed(AuthError.RequiresLogin)
                }
                if (!response.isSuccess) {
                    val error = response.error?.let { AuthErrorNormalizer.normalize(it) }
                        ?: AuthError.OAuthError("refresh_failed")
                    return LoginResult.Failed(error)
                }
                if (!response.isAcceptableTokenType) {
                    tokenStore.clear()
                    return LoginResult.Failed(AuthError.OAuthError("unsupported_token_type"))
                }
                val refreshed = AuthSession(
                    accessToken = requireNotNull(response.accessToken),
                    refreshToken = response.refreshToken ?: session.refreshToken,
                    idToken = response.idToken ?: session.idToken,
                    expiresInSeconds = response.expiresIn ?: session.expiresInSeconds,
                    scope = response.scope ?: session.scope,
                    tokenType = response.tokenType ?: session.tokenType
                )
                tokenStore.write(refreshed)
                LoginResult.Authenticated(refreshed)
            },
            onFailure = { error -> LoginResult.Failed(error as AuthError) }
        )
    }

    override fun revoke(session: AuthSession): LoginResult.LoggedOut {
        val revocationEndpoint = discovery.revocationEndpoint
        if (revocationEndpoint != null && session.refreshToken != null) {
            val params = mapOf(
                "token" to session.refreshToken,
                "token_type_hint" to "refresh_token",
                "client_id" to config.clientId
            )
            tokenTransport.revokeToken(revocationEndpoint, params)
        }
        tokenStore.clear()
        return LoginResult.LoggedOut
    }

    private fun onTokenResponse(response: TokenResponse): LoginResult {
        if (!response.isSuccess) {
            val error = response.error?.let { AuthErrorNormalizer.normalize(it) }
                ?: AuthError.OAuthError("token_failed")
            return LoginResult.Failed(error)
        }
        if (!response.isAcceptableTokenType) {
            return LoginResult.Failed(AuthError.OAuthError("unsupported_token_type"))
        }
        val session = AuthSession(
            accessToken = requireNotNull(response.accessToken),
            refreshToken = response.refreshToken,
            idToken = response.idToken,
            expiresInSeconds = response.expiresIn ?: 3600,
            scope = response.scope ?: config.scopes.joinToString(" "),
            tokenType = response.tokenType ?: "Bearer"
        )
        tokenStore.write(session)
        return LoginResult.Authenticated(session)
    }
}
