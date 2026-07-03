package dev.defdo.mobile.auth

fun runPKCETest() {
    val fixtures = AuthTestFixtures.loadJsonArray("pkce.test_vectors.json")
    check(fixtures.isNotEmpty()) { "no test vectors found" }

    for (vector in fixtures) {
        val verifier = vector["verifier"] as String
        val expectedChallenge = vector["challenge"] as String
        val method = vector["method"] as String

        check(method == "S256") { "expected S256 method" }
        check(PKCE.isValidVerifier(verifier)) { "verifier should be valid: $verifier" }
        check(PKCE.s256Challenge(verifier) == expectedChallenge) {
            "challenge mismatch: expected $expectedChallenge, got ${PKCE.s256Challenge(verifier)}"
        }
    }

    val generated = PKCE.generateVerifier()
    check(PKCE.isValidVerifier(generated)) { "generated verifier should be valid" }
    check(generated.length in 43..128) { "generated verifier length ${generated.length} not in range" }
    val generatedChallenge = PKCE.s256Challenge(generated)
    check(generatedChallenge.isNotBlank()) { "generated challenge should not be blank" }
    check(!generatedChallenge.endsWith("=")) { "challenge should be base64url without padding" }
    check(!generatedChallenge.contains("+") && !generatedChallenge.contains("/")) {
        "challenge should use URL-safe characters only"
    }
}
