package dev.defdo.mobile.auth

fun runDiscoveryContractTest() {
    val oidcFixture = AuthTestFixtures.loadJson("discovery.oidc.mi-omv.fixture.json")
    val oidcDoc = OAuthDiscoveryDocument(
        issuer = oidcFixture["issuer"] as String,
        authorizationEndpoint = oidcFixture["authorization_endpoint"] as String,
        tokenEndpoint = oidcFixture["token_endpoint"] as String,
        revocationEndpoint = oidcFixture["revocation_endpoint"] as? String,
        userinfoEndpoint = oidcFixture["userinfo_endpoint"] as? String,
        jwksUri = oidcFixture["jwks_uri"] as? String,
        codeChallengeMethodsSupported = (oidcFixture["code_challenge_methods_supported"] as? List<*>)?.map { it.toString() },
        scopesSupported = (oidcFixture["scopes_supported"] as? List<*>)?.map { it.toString() },
        grantTypesSupported = (oidcFixture["grant_types_supported"] as? List<*>)?.map { it.toString() },
        responseTypesSupported = (oidcFixture["response_types_supported"] as? List<*>)?.map { it.toString() },
        tokenEndpointAuthMethodsSupported = (oidcFixture["token_endpoint_auth_methods_supported"] as? List<*>)?.map { it.toString() }
    )
    check(oidcDoc.validate("https://idp.mi-omv.com") == null) { "OIDC discovery from idp.mi-omv.com should pass validation" }
    check(oidcDoc.authorizationEndpoint == "https://idp.mi-omv.com/openid/authorize") { "OIDC authorization_endpoint must be /openid/authorize" }
    check(oidcDoc.userinfoEndpoint != null) { "OIDC discovery must include userinfo_endpoint" }
    check(oidcDoc.tokenEndpointAuthMethodsSupported?.contains("none") == true) { "token_endpoint_auth_methods_supported must include none for public clients" }

    val oauthAsFixture = AuthTestFixtures.loadJson("discovery.oauth_as.mi-omv.fixture.json")
    val oauthAsDoc = OAuthDiscoveryDocument(
        issuer = oauthAsFixture["issuer"] as String,
        authorizationEndpoint = oauthAsFixture["authorization_endpoint"] as String,
        tokenEndpoint = oauthAsFixture["token_endpoint"] as String,
        revocationEndpoint = oauthAsFixture["revocation_endpoint"] as? String,
        userinfoEndpoint = oauthAsFixture["userinfo_endpoint"] as? String,
        jwksUri = oauthAsFixture["jwks_uri"] as? String,
        codeChallengeMethodsSupported = (oauthAsFixture["code_challenge_methods_supported"] as? List<*>)?.map { it.toString() },
        scopesSupported = (oauthAsFixture["scopes_supported"] as? List<*>)?.map { it.toString() },
        grantTypesSupported = (oauthAsFixture["grant_types_supported"] as? List<*>)?.map { it.toString() },
        responseTypesSupported = (oauthAsFixture["response_types_supported"] as? List<*>)?.map { it.toString() },
        tokenEndpointAuthMethodsSupported = (oauthAsFixture["token_endpoint_auth_methods_supported"] as? List<*>)?.map { it.toString() }
    )
    check(oauthAsDoc.validate("https://idp.mi-omv.com") == null) { "OAuth AS metadata from idp.mi-omv.com should pass validation" }
    check(oauthAsDoc.authorizationEndpoint == "https://idp.mi-omv.com/oauth/authorize") { "OAuth AS authorization_endpoint must be /oauth/authorize" }
    check(oauthAsDoc.userinfoEndpoint == null) { "OAuth AS metadata must not require userinfo_endpoint" }
    check(oauthAsDoc.tokenEndpoint == oidcDoc.tokenEndpoint) { "token_endpoint must be identical between discovery documents" }
    check(oauthAsDoc.revocationEndpoint == oidcDoc.revocationEndpoint) { "revocation_endpoint must be identical between discovery documents" }

    val successFixture = AuthTestFixtures.loadJson("discovery.success.fixture.json")
    val valid = OAuthDiscoveryDocument(
        issuer = successFixture["issuer"] as String,
        authorizationEndpoint = successFixture["authorization_endpoint"] as String,
        tokenEndpoint = successFixture["token_endpoint"] as String,
        revocationEndpoint = successFixture["revocation_endpoint"] as? String,
        userinfoEndpoint = successFixture["userinfo_endpoint"] as? String,
        jwksUri = successFixture["jwks_uri"] as? String,
        codeChallengeMethodsSupported = (successFixture["code_challenge_methods_supported"] as? List<*>)?.map { it.toString() },
        scopesSupported = (successFixture["scopes_supported"] as? List<*>)?.map { it.toString() }
    )
    check(valid.validate() == null) { "valid discovery should pass validation" }

    val invalidFixture = AuthTestFixtures.loadJson("discovery.invalid.fixture.json")
    val invalid = OAuthDiscoveryDocument(
        issuer = invalidFixture["issuer"] as? String,
        authorizationEndpoint = invalidFixture["authorization_endpoint"] as? String,
        tokenEndpoint = null,
        revocationEndpoint = null
    )
    val error = invalid.validate()
    check(error is AuthError.InvalidDiscovery) { "invalid discovery should fail validation" }

    val missingIssuer = OAuthDiscoveryDocument(
        issuer = "",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token"
    )
    check(missingIssuer.validate() is AuthError.InvalidDiscovery) { "missing issuer should fail" }

    val missingAuth = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = null,
        tokenEndpoint = "https://auth.example/token"
    )
    check(missingAuth.validate() is AuthError.InvalidDiscovery) { "missing authorization_endpoint should fail" }

    val missingToken = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = ""
    )
    check(missingToken.validate() is AuthError.InvalidDiscovery) { "missing token_endpoint should fail" }

    val withOptionals = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        revocationEndpoint = "https://auth.example/revoke",
        userinfoEndpoint = "https://auth.example/userinfo",
        jwksUri = "https://auth.example/jwks",
        codeChallengeMethodsSupported = listOf("S256", "plain")
    )
    check(withOptionals.validate() == null) { "discovery with optional fields should pass" }

    val nonHttps = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "http://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token"
    )
    check(nonHttps.validate() is AuthError.InvalidDiscovery) { "non-HTTPS endpoint should fail" }

    val issuerMismatch = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token"
    )
    check(issuerMismatch.validate("https://other.example") is AuthError.InvalidDiscovery) { "issuer mismatch should fail" }

    val hostMismatch = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://evil.example/token"
    )
    check(hostMismatch.validate("https://auth.example") is AuthError.InvalidDiscovery) { "endpoint host mismatch should fail" }

    val missingS256 = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        codeChallengeMethodsSupported = listOf("plain")
    )
    check(missingS256.validate() is AuthError.InvalidDiscovery) { "missing S256 in code_challenge_methods_supported should fail" }

    val missingCodeResponseType = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        responseTypesSupported = listOf("token")
    )
    check(missingCodeResponseType.validate() is AuthError.InvalidDiscovery) { "missing code response_type should fail" }

    val missingAuthzGrant = OAuthDiscoveryDocument(
        issuer = "https://auth.example",
        authorizationEndpoint = "https://auth.example/authorize",
        tokenEndpoint = "https://auth.example/token",
        grantTypesSupported = listOf("client_credentials")
    )
    check(missingAuthzGrant.validate() is AuthError.InvalidDiscovery) { "missing authorization_code grant_type should fail" }
}
