package dev.defdo.mobile.auth

/**
 * Disabled-by-default smoke harness for live testing against a defdo_auth dev issuer.
 *
 * Enable by setting environment variables:
 *   DEFDO_DEV_ISSUER       (required, e.g. https://auth.dev.defdo.example)
 *   DEFDO_DEV_CLIENT_ID    (default: dev-client)
 *   DEFDO_DEV_REDIRECT_URI (default: https://app.defdo.example/oauth/callback)
 *   DEFDO_DEV_SCOPES       (space-separated, default: openid profile offline_access)
 *
 * When enabled, this harness performs discovery loading, PKCE generation,
 * authorization URL construction, and synthetic token/refresh wiring against
 * the configured issuer. It does not perform a real browser login.
 *
 * Sensitive values (tokens, codes, verifiers) are NEVER printed.
 */
object DevIssuerSmokeHarness {

    fun enabled(): Boolean {
        return System.getenv("DEFDO_DEV_ISSUER")?.isNotBlank() == true
    }

    fun run() {
        if (!enabled()) {
            println("[dev-smoke] disabled; set DEFDO_DEV_ISSUER to enable")
            return
        }

        val issuer = System.getenv("DEFDO_DEV_ISSUER")!!
        val clientId = System.getenv("DEFDO_DEV_CLIENT_ID") ?: "dev-client"
        val redirectUri = System.getenv("DEFDO_DEV_REDIRECT_URI") ?: "https://app.defdo.example/oauth/callback"
        val scopes = (System.getenv("DEFDO_DEV_SCOPES") ?: "openid profile offline_access").split(" ")

        println("[dev-smoke] starting against $issuer")

        val discoveryUrl = "$issuer/.well-known/openid-configuration"
        val discovery = try {
            val connection = java.net.URL(discoveryUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val map = parseSimpleJson(body)
            println("[dev-smoke] discovery loaded")
            OAuthDiscoveryDocument(
                issuer = map["issuer"] as? String,
                authorizationEndpoint = map["authorization_endpoint"] as? String,
                tokenEndpoint = map["token_endpoint"] as? String,
                revocationEndpoint = map["revocation_endpoint"] as? String,
                userinfoEndpoint = map["userinfo_endpoint"] as? String,
                jwksUri = map["jwks_uri"] as? String,
                codeChallengeMethodsSupported = (map["code_challenge_methods_supported"] as? List<*>)?.map { it.toString() },
                scopesSupported = (map["scopes_supported"] as? List<*>)?.map { it.toString() }
            )
        } catch (e: Exception) {
            println("[dev-smoke] FAILED to load discovery: ${e.message}")
            return
        }

        discovery.validate(issuer)?.let { error ->
            println("[dev-smoke] FAILED discovery validation: $error")
            return
        }

        println("[dev-smoke] discovery valid (issuer=${discovery.issuer})")

        val config = AuthConfig(
            clientId = clientId,
            discoveryUrl = discoveryUrl,
            redirectUri = redirectUri,
            scopes = scopes
        )

        val transport = UrlConnectionTokenHttpTransport()
        val store = SecureTokenStore(InMemorySecureStorageAdapter())
        val client = DefdoAuthMobileClientImpl(
            config,
            discovery,
            FakeBrowserAuthAdapter(),
            store,
            transport
        )

        val state = java.util.UUID.randomUUID().toString()
        val verifier = PKCE.generateVerifier()
        val nonce = java.util.UUID.randomUUID().toString()
        val request = LoginRequest(config, state, nonce, verifier)
        val authUrl = AuthorizationRequestBuilder.url(
            requireNotNull(discovery.authorizationEndpoint),
            request
        )

        println("[dev-smoke] authorization URL built (state=${state.length} chars, verifier=${verifier.length} chars)")
        println("[dev-smoke] authorization URL length: ${authUrl.length}")
        println("[dev-smoke] manual browser login required. Open the URL in a browser and copy the callback URL.")

        println("[dev-smoke] after callback, run token exchange via callback URL parameter.")
        println("[dev-smoke] smoke harness ready for callback handoff.")
    }
}

fun main() {
    DevIssuerSmokeHarness.run()
}
