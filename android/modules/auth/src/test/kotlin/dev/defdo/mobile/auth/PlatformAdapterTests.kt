package dev.defdo.mobile.auth

fun runPlatformAdapterTest() {
    val config = AuthConfig(
        clientId = "mobile-client",
        discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
        redirectUri = "https://login.defdo-telecom.example/mobile/oauth/callback",
        scopes = listOf("openid", "profile", "offline_access")
    )
    val discovery = OAuthDiscoveryDocument(
        issuer = "https://auth.defdo.example",
        authorizationEndpoint = "https://auth.defdo.example/oauth/authorize",
        tokenEndpoint = "https://auth.defdo.example/oauth/token",
        revocationEndpoint = "https://auth.defdo.example/oauth/revoke"
    )

    val browser = FakeBrowserAuthAdapter()
    val storage = InMemorySecureStorageAdapter()
    val tokenStore = SecureTokenStore(storage)
    val successToken = TokenResponse(
        accessToken = "ACCESS_TOKEN_VALUE",
        refreshToken = "REFRESH_TOKEN_VALUE",
        idToken = "ID_TOKEN_VALUE",
        tokenType = "Bearer",
        expiresIn = 3600,
        scope = "openid profile offline_access",
        error = null
    )
    val transport = FakeTokenHttpTransport(exchangeResponse = successToken)
    val client = DefdoAuthMobileClientImpl(config, discovery, browser, tokenStore, transport)

    val state = "STATE_VALUE"
    val verifier = PKCE.generateVerifier()
    val request = LoginRequest(config, state, nonce = "NONCE_VALUE", codeVerifier = verifier)

    val pending = client.startLogin(request)
    check(browser.openedUrls.isNotEmpty()) { "browser adapter should receive URL" }
    check(browser.openedUrls.last().startsWith(discovery.authorizationEndpoint!!)) { "URL should use authorization endpoint" }
    check(browser.openedUrls.last().contains("code_challenge=")) { "URL should contain PKCE challenge" }
    @Suppress("USELESS_IS_CHECK")
    check(pending is LoginResult.PendingBrowser) { "login should return PendingBrowser" }

    val callback = "${config.redirectUri}?code=AUTH_CODE&state=$state"
    val result = client.handleCallback(callback, state, config.redirectUri)
    check(result is LoginResult.Authenticated) { "callback should authenticate, got $result" }
    check(result.session.accessToken == "ACCESS_TOKEN_VALUE") { "access token mismatch" }

    val exchangeCall = transport.exchangeCalls.last()
    check(exchangeCall["grant_type"] == "authorization_code") { "grant_type mismatch" }
    check(exchangeCall["code"] == "AUTH_CODE") { "code mismatch" }
    check(exchangeCall["code_verifier"] == verifier) { "code_verifier mismatch" }
    check(exchangeCall["client_id"] == config.clientId) { "client_id mismatch" }
    check(exchangeCall["redirect_uri"] == config.redirectUri) { "redirect_uri mismatch" }

    val storedSession = client.currentSession()
    check(storedSession != null) { "token store should have session" }
    check(storedSession!!.accessToken == "ACCESS_TOKEN_VALUE") { "stored access token mismatch" }

    val rotatedToken = RefreshResponse(
        accessToken = "NEW_ACCESS_TOKEN_VALUE",
        refreshToken = "NEW_REFRESH_TOKEN_VALUE",
        idToken = null,
        tokenType = "Bearer",
        expiresIn = 3600,
        scope = "openid profile offline_access",
        error = null
    )
    val refreshTransport = FakeTokenHttpTransport(refreshResponse = rotatedToken)
    val refreshClient = DefdoAuthMobileClientImpl(config, discovery, browser, tokenStore, refreshTransport)
    val refreshResult = refreshClient.refresh(storedSession)
    check(refreshResult is LoginResult.Authenticated) { "refresh should succeed" }
    check(refreshResult.session.accessToken == "NEW_ACCESS_TOKEN_VALUE") { "rotated access token mismatch" }
    check(refreshResult.session.refreshToken == "NEW_REFRESH_TOKEN_VALUE") { "refresh token should rotate" }

    val invalidGrant = RefreshResponse(
        accessToken = null,
        refreshToken = null,
        idToken = null,
        tokenType = null,
        expiresIn = null,
        scope = null,
        error = "invalid_grant"
    )
    val invalidGrantTransport = FakeTokenHttpTransport(refreshResponse = invalidGrant)
    val invalidGrantClient = DefdoAuthMobileClientImpl(config, discovery, browser, tokenStore, invalidGrantTransport)
    val invalidGrantResult = invalidGrantClient.refresh(refreshResult.session)
    check(invalidGrantResult is LoginResult.Failed) { "invalid_grant should fail" }
    check(invalidGrantResult.error == AuthError.RequiresLogin) { "invalid_grant should normalize to RequiresLogin" }
    check(invalidGrantClient.currentSession() == null) { "invalid_grant should clear local session" }

    val revokeTransport = FakeTokenHttpTransport(revokeResult = Result.failure(AuthError.Retryable))
    val revokeStore = SecureTokenStore(InMemorySecureStorageAdapter())
    revokeStore.write(refreshResult.session)
    val revokeClient = DefdoAuthMobileClientImpl(config, discovery, browser, revokeStore, revokeTransport)
    val sessionBeforeRevoke = requireNotNull(revokeClient.currentSession())
    val revokeResult = revokeClient.revoke(sessionBeforeRevoke)
    check(revokeResult == LoginResult.LoggedOut) { "revoke should return LoggedOut" }
    check(revokeClient.currentSession() == null) { "revoke should clear local session even on remote failure" }

    val noRevokeDiscovery = OAuthDiscoveryDocument(
        issuer = discovery.issuer,
        authorizationEndpoint = discovery.authorizationEndpoint,
        tokenEndpoint = discovery.tokenEndpoint,
        revocationEndpoint = null
    )
    val noRevokeTransport = FakeTokenHttpTransport()
    val noRevokeStore = SecureTokenStore(InMemorySecureStorageAdapter())
    val noRevokeClient = DefdoAuthMobileClientImpl(config, noRevokeDiscovery, browser, noRevokeStore, noRevokeTransport)
    val sessionForNoRevoke = AuthSession(
        accessToken = "a",
        refreshToken = "b",
        idToken = null,
        expiresInSeconds = 3600,
        scope = "openid"
    )
    noRevokeClient.revoke(sessionForNoRevoke)
    check(noRevokeClient.currentSession() == null) { "logout should clear local session without revocation endpoint" }
}

fun runTokenStoreAdapterTest() {
    val storage = InMemorySecureStorageAdapter()
    val store = SecureTokenStore(storage)
    val session = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        idToken = "id",
        expiresInSeconds = 3600,
        scope = "openid",
        tokenType = "Bearer"
    )
    store.write(session)
    val read = store.read()
    check(read == session) { "stored session should round-trip: read=$read" }
    store.clear()
    check(store.read() == null) { "store should be empty after clear" }
}

fun runTokenEnvelopeCorruptTest() {
    val storage = InMemorySecureStorageAdapter()
    storage.put("defdo_auth_session", "not json".toByteArray(Charsets.UTF_8))
    val store = SecureTokenStore(storage)
    check(store.read() == null) { "corrupt envelope should return null" }
}

fun runTokenEnvelopeFutureSchemaTest() {
    val storage = InMemorySecureStorageAdapter()
    val futureJson = """{"schema_version":999,"access_token":"a","token_type":"Bearer","expires_in":3600,"captured_at":0,"scope":"openid"}"""
    storage.put("defdo_auth_session", futureJson.toByteArray(Charsets.UTF_8))
    val store = SecureTokenStore(storage)
    check(store.read() == null) { "future schema_version should be rejected" }
}

fun runTokenEnvelopeMissingOptionalsTest() {
    val storage = InMemorySecureStorageAdapter()
    val session = AuthSession(
        accessToken = "access",
        refreshToken = null,
        idToken = null,
        expiresInSeconds = 3600,
        scope = "openid",
        tokenType = "Bearer"
    )
    val store = SecureTokenStore(storage)
    store.write(session)
    val read = store.read()
    check(read != null) { "session should be readable" }
    check(read!!.refreshToken == null) { "missing optional refreshToken should be null" }
    check(read.idToken == null) { "missing optional idToken should be null" }
}

fun runTokenTypeEnforcementTest() {
    val successFixture = AuthTestFixtures.loadJson("token.success.fixture.json")
    val token = TokenResponse.parse(successFixture)
    check(token.isSuccess) { "should be success" }
    check(token.tokenType == "Bearer") { "token_type must be Bearer, got ${token.tokenType}" }

    val nonBearer = TokenResponse(
        accessToken = "abc",
        refreshToken = null,
        idToken = null,
        tokenType = "MAC",
        expiresIn = 3600,
        scope = "openid",
        error = null
    )
    check(nonBearer.tokenType != "Bearer") { "MAC should not be Bearer" }

    val config = AuthConfig(
        clientId = "mobile-client",
        discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
        redirectUri = "https://app.example/callback",
        scopes = listOf("openid")
    )
    val discovery = OAuthDiscoveryDocument(
        issuer = "https://auth.defdo.example",
        authorizationEndpoint = "https://auth.defdo.example/oauth/authorize",
        tokenEndpoint = "https://auth.defdo.example/oauth/token",
        revocationEndpoint = null
    )
    val browser = FakeBrowserAuthAdapter()
    val storage = InMemorySecureStorageAdapter()
    val transport = FakeTokenHttpTransport(exchangeResponse = nonBearer)
    val client = DefdoAuthMobileClientImpl(config, discovery, browser, SecureTokenStore(storage), transport)
    val state = "s"
    client.startLogin(LoginRequest(config, state, null, PKCE.generateVerifier()))
    val result = client.handleCallback(
        "${config.redirectUri}?code=c&state=$state",
        state,
        config.redirectUri
    )
    check(result is LoginResult.Failed) { "non-Bearer token should be rejected before storing" }
    check(result.error is AuthError.OAuthError) { "non-Bearer should return OAuthError" }
    check((result.error as AuthError.OAuthError).code == "unsupported_token_type") { "error should be unsupported_token_type" }

    val stored = client.currentSession()
    check(stored == null) { "non-Bearer token must not persist" }
}

fun runTokenTypeBearerPersistsTest() {
    val config = AuthConfig(
        clientId = "mobile-client",
        discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
        redirectUri = "https://app.example/callback",
        scopes = listOf("openid")
    )
    val discovery = OAuthDiscoveryDocument(
        issuer = "https://auth.defdo.example",
        authorizationEndpoint = "https://auth.defdo.example/oauth/authorize",
        tokenEndpoint = "https://auth.defdo.example/oauth/token",
        revocationEndpoint = null
    )
    val browser = FakeBrowserAuthAdapter()
    val storage = InMemorySecureStorageAdapter()
    val bearerToken = TokenResponse(
        accessToken = "abc", refreshToken = "r", idToken = null,
        tokenType = "Bearer", expiresIn = 3600, scope = "openid", error = null
    )
    val transport = FakeTokenHttpTransport(exchangeResponse = bearerToken)
    val client = DefdoAuthMobileClientImpl(config, discovery, browser, SecureTokenStore(storage), transport)
    val state = "s"
    client.startLogin(LoginRequest(config, state, null, PKCE.generateVerifier()))
    val result = client.handleCallback(
        "${config.redirectUri}?code=c&state=$state",
        state,
        config.redirectUri
    )
    check(result is LoginResult.Authenticated) { "Bearer token should persist" }
    check(client.currentSession() != null) { "Bearer session should be stored" }
}

fun runTokenTypeAbsentPersistsTest() {
    val config = AuthConfig(
        clientId = "mobile-client",
        discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
        redirectUri = "https://app.example/callback",
        scopes = listOf("openid")
    )
    val discovery = OAuthDiscoveryDocument(
        issuer = "https://auth.defdo.example",
        authorizationEndpoint = "https://auth.defdo.example/oauth/authorize",
        tokenEndpoint = "https://auth.defdo.example/oauth/token",
        revocationEndpoint = null
    )
    val browser = FakeBrowserAuthAdapter()
    val storage = InMemorySecureStorageAdapter()
    val absentTypeToken = TokenResponse(
        accessToken = "abc", refreshToken = null, idToken = null,
        tokenType = null, expiresIn = 3600, scope = "openid", error = null
    )
    val transport = FakeTokenHttpTransport(exchangeResponse = absentTypeToken)
    val client = DefdoAuthMobileClientImpl(config, discovery, browser, SecureTokenStore(storage), transport)
    val state = "s"
    client.startLogin(LoginRequest(config, state, null, PKCE.generateVerifier()))
    val result = client.handleCallback(
        "${config.redirectUri}?code=c&state=$state",
        state,
        config.redirectUri
    )
    check(result is LoginResult.Authenticated) { "absent token_type should persist" }
    check(client.currentSession() != null) { "absent token_type session should be stored" }
}

fun runDiscoveryHardeningNotes() {
    val discovery = AuthTestFixtures.loadJson("discovery.success.fixture.json")
    check(discovery["issuer"] != null) { "issuer must be present" }
    check(discovery["authorization_endpoint"] != null) { "authorization_endpoint must be present" }
    check(discovery["token_endpoint"] != null) { "token_endpoint must be present" }

    @Suppress("UNCHECKED_CAST")
    val supportedMethods = discovery["code_challenge_methods_supported"] as? List<String>
    if (supportedMethods != null) {
        check("S256" in supportedMethods) { "discovery code_challenge_methods_supported should include S256 when present" }
    }
}

fun runCallbackHandoffTest() {
    val config = AuthConfig(
        clientId = "c",
        discoveryUrl = "https://auth.example/.well-known",
        redirectUri = "https://app.example/callback",
        scopes = listOf("openid")
    )
    val discovery = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        revocationEndpoint = null
    )
    val browser = FakeBrowserAuthAdapter()
    val storage = InMemorySecureStorageAdapter()
    val token = TokenResponse(
        accessToken = "a", refreshToken = "r", idToken = null,
        tokenType = "Bearer", expiresIn = 3600, scope = "openid", error = null
    )
    val transport = FakeTokenHttpTransport(exchangeResponse = token)
    val client = DefdoAuthMobileClientImpl(config, discovery, browser, SecureTokenStore(storage), transport)
    val state = "s"
    client.startLogin(LoginRequest(config, state, null, PKCE.generateVerifier()))
    val result = client.handleCallback(
        "${config.redirectUri}?code=c&state=$state",
        state,
        config.redirectUri
    )
    check(result is LoginResult.Authenticated) { "App Link handoff should authenticate" }
}
