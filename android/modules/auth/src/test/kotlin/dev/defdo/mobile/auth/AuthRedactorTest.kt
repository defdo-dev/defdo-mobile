package dev.defdo.mobile.auth

fun runAuthRedactorTest() {
    val rulesFixture = AuthTestFixtures.loadJson("auth_redaction_rules.json")
    @Suppress("UNCHECKED_CAST")
    val redactKeys = rulesFixture["redact_keys"] as List<String>
    @Suppress("UNCHECKED_CAST")
    val redactQueryParams = rulesFixture["redact_query_params"] as List<String>
    @Suppress("UNCHECKED_CAST")
    val redactHeaders = rulesFixture["redact_headers"] as List<String>
    val replacement = rulesFixture["replacement"] as String

    check("access_token" in redactKeys)
    check("refresh_token" in redactKeys)
    check("id_token" in redactKeys)
    check("code" in redactKeys)
    check("code_verifier" in redactKeys)
    check("claims" in redactKeys)
    check("authorization" in redactHeaders)
    check("set-cookie" in redactHeaders)

    val redacted = AuthRedactor.redact(
        "https://app/callback?code=verysecret&state=ok access_token=verytoken"
    )
    check(!redacted.contains("verysecret")) { "code value should be redacted" }
    check(!redacted.contains("verytoken")) { "access_token value should be redacted" }
    check(redacted.contains("[REDACTED]")) { "should contain redaction marker" }

    val jsonRedacted = AuthRedactor.redact(
        """{"access_token":"abc123","refresh_token":"xyz789"}"""
    )
    check(!jsonRedacted.contains("abc123")) { "JSON access_token should be redacted" }
    check(!jsonRedacted.contains("xyz789")) { "JSON refresh_token should be redacted" }

    val emptyStringRedacted = AuthRedactor.redact(
        """{"access_token":"","refresh_token":"","id_token":""}"""
    )
    check(emptyStringRedacted.contains("\"access_token\":\"[REDACTED]\"")) {
        "empty access_token should be redacted: $emptyStringRedacted"
    }
    check(emptyStringRedacted.contains("\"refresh_token\":\"[REDACTED]\"")) {
        "empty refresh_token should be redacted"
    }
    check(emptyStringRedacted.contains("\"id_token\":\"[REDACTED]\"")) {
        "empty id_token should be redacted"
    }

    val headerRedacted = AuthRedactor.redact("""
        Authorization: Bearer some-token-value
        Set-Cookie: session=session-value
    """.trimIndent())
    check(!headerRedacted.contains("some-token-value")) { "Authorization header should be redacted" }
    check(!headerRedacted.contains("session-value")) { "Set-Cookie header should be redacted" }
    check(headerRedacted.contains("Authorization: [REDACTED]")) { "Authorization redaction marker missing" }
    check(headerRedacted.contains("Set-Cookie: [REDACTED]")) { "Set-Cookie redaction marker missing" }

    val callbackRedacted = AuthRedactor.redact(
        "https://app/callback?code=abc&state=def&id_token=ghi&access_token=jkl"
    )
    check(!callbackRedacted.contains("abc")) { "callback code should be redacted" }
    check(!callbackRedacted.contains("ghi")) { "callback id_token should be redacted" }
    check(!callbackRedacted.contains("jkl")) { "callback access_token should be redacted" }

    val claimsStringRedacted = AuthRedactor.redact(
        """{"claims":"{\"email\":\"test@test.com\"}"}"""
    )
    check(!claimsStringRedacted.contains("test@test.com")) { "claims string should be redacted" }

    val claimsObjectRedacted = AuthRedactor.redact(
        """{"claims":{"email":"test@test.com","sub":"12345"}}"""
    )
    check(!claimsObjectRedacted.contains("test@test.com")) { "claims object email should be redacted" }
    check(!claimsObjectRedacted.contains("12345")) { "claims object sub should be redacted" }

    val claimsArrayRedacted = AuthRedactor.redact(
        """{"claims":["openid","profile"]}"""
    )
    check(!claimsArrayRedacted.contains("openid")) { "claims array values should be redacted" }
    check(!claimsArrayRedacted.contains("profile")) { "claims array values should be redacted" }
}
