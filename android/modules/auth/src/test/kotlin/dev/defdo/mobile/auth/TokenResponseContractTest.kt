package dev.defdo.mobile.auth

fun runTokenResponseContractTest() {
    val successFixture = AuthTestFixtures.loadJson("token.success.fixture.json")
    val token = TokenResponse.parse(successFixture)
    check(token.isSuccess) { "success fixture should parse as success" }
    check(token.accessToken == "ACCESS_TOKEN_VALUE") { "access_token mismatch" }
    check(token.refreshToken == "REFRESH_TOKEN_VALUE") { "refresh_token mismatch" }
    check(token.idToken == "ID_TOKEN_VALUE") { "id_token mismatch" }
    check(token.tokenType == "Bearer") { "token_type mismatch" }
    check(token.expiresIn == 3600L) { "expires_in mismatch" }
    check(token.scope == "openid profile offline_access selfcare:read") { "scope mismatch" }
    check(token.error == null) { "error should be null" }

    val errorFixture = AuthTestFixtures.loadJson("token.error.fixture.json")
    val errorToken = TokenResponse.parse(errorFixture)
    check(!errorToken.isSuccess) { "error fixture should not be success" }
    check(errorToken.error == "invalid_request") { "error mismatch" }
    val normalized = AuthErrorNormalizer.normalize(errorToken.error!!)
    check(normalized is AuthError.OAuthError) { "invalid_request should normalize to OAuthError" }
}
