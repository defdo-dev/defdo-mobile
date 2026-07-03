package dev.defdo.mobile.auth

fun runAuthorizationRequestBuilderTest() {
    val expected = AuthTestFixtures.loadJson("authorization_request.expected.json")
    val config = AuthConfig(
        clientId = expected["client_id"] as String,
        discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
        redirectUri = expected["redirect_uri"] as String,
        scopes = (expected["scope"] as String).split(" "),
        useNonce = true
    )
    val request = LoginRequest(
        config = config,
        state = expected["state"] as String,
        nonce = expected["nonce"] as String,
        codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    )

    val params = AuthorizationRequestBuilder.parameters(request)
    check(params["response_type"] == "code") { "response_type must be code" }
    check(params["code_challenge_method"] == "S256") { "code_challenge_method must be S256" }
    check(params["client_id"] == expected["client_id"]) { "client_id mismatch" }
    check(params["redirect_uri"] == expected["redirect_uri"]) { "redirect_uri mismatch" }
    check(params["scope"] == expected["scope"]) { "scope mismatch" }
    check(params["state"] == expected["state"]) { "state mismatch" }
    check(params["nonce"] == expected["nonce"]) { "nonce mismatch" }
    check(params["code_challenge"] == expected["code_challenge"]) { "code_challenge mismatch" }
    check(params.size == 8) { "expected 8 params (including nonce), got ${params.size}" }

    val url = AuthorizationRequestBuilder.url(
        "https://auth.defdo.example/oauth/authorize",
        request
    )
    check(url.startsWith("https://auth.defdo.example/oauth/authorize?"))
    check(url.contains("response_type=code"))
    check(url.contains("code_challenge_method=S256"))
    check(!url.contains("+")) { "URL must use %20 not + for spaces (RFC3986)" }

    val golden = AuthTestFixtures.loadJson("authorization_request.golden_url.json")
    val goldenRequest = LoginRequest(
        config = AuthConfig(
            clientId = golden["client_id"] as String,
            discoveryUrl = "https://auth.defdo.example/.well-known/openid-configuration",
            redirectUri = golden["redirect_uri"] as String,
            scopes = (golden["scope"] as String).split(" "),
            useNonce = true
        ),
        state = golden["state"] as String,
        nonce = golden["nonce"] as String,
        codeVerifier = golden["code_verifier"] as String
    )
    val goldenUrl = AuthorizationRequestBuilder.url(
        golden["authorization_endpoint"] as String,
        goldenRequest
    )
    val expectedGoldenUrl = golden["expected_url"] as String
    check(goldenUrl == expectedGoldenUrl) {
        "golden URL mismatch.\n  expected: $expectedGoldenUrl\n  actual:   $goldenUrl"
    }

    try {
        AuthorizationRequestBuilder.parameters(
            LoginRequest(AuthConfig("", "", "", emptyList()), "state", null, "")
        )
        check(false) { "should reject missing client_id" }
    } catch (_: IllegalArgumentException) { }

    try {
        AuthorizationRequestBuilder.parameters(
            LoginRequest(AuthConfig("client", "", "redirect", listOf("openid")), "state", null, "")
        )
        check(false) { "should reject missing redirect_uri" }
    } catch (_: IllegalArgumentException) { }

    try {
        AuthorizationRequestBuilder.parameters(
            LoginRequest(AuthConfig("client", "", "redirect", emptyList()), "state", null, "validVerifier123")
        )
        check(false) { "should reject empty scopes" }
    } catch (_: IllegalArgumentException) { }

    try {
        AuthorizationRequestBuilder.url("", request)
        check(false) { "should reject missing authorization_endpoint" }
    } catch (_: IllegalArgumentException) { }
}
