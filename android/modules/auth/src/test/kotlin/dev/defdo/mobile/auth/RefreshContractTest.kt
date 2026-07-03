package dev.defdo.mobile.auth

fun runRefreshContractTest() {
    val successFixture = AuthTestFixtures.loadJson("refresh.success.fixture.json")
    val response = RefreshResponse.parse(successFixture)
    check(response.isSuccess) { "refresh success should be success" }
    check(response.hasRotatedRefreshToken) { "should have new refresh token" }
    check(response.refreshToken == "NEW_REFRESH_TOKEN_VALUE") { "new refresh_token should be present" }
    check(response.tokenType == "Bearer") { "token_type should be Bearer" }

    val invalidGrantFixture = AuthTestFixtures.loadJson("refresh.invalid_grant.fixture.json")
    val invalidGrantResponse = RefreshResponse.parse(invalidGrantFixture)
    check(!invalidGrantResponse.isSuccess) { "invalid_grant should not be success" }
    check(invalidGrantResponse.requiresLogin) { "invalid_grant should require login" }
    check(invalidGrantFixture["clear_local_session"] == true) { "invalid_grant should clear local session" }
    val normalized = AuthErrorNormalizer.normalize("invalid_grant")
    check(normalized == AuthError.RequiresLogin) { "invalid_grant should normalize to RequiresLogin" }

    val refreshWithoutNewRefreshToken = RefreshResponse(
        accessToken = "new-access",
        refreshToken = null,
        idToken = null,
        tokenType = "Bearer",
        expiresIn = 3600,
        scope = "openid",
        error = null
    )
    check(refreshWithoutNewRefreshToken.isSuccess) { "refresh without new refresh token should still be success" }
    check(!refreshWithoutNewRefreshToken.hasRotatedRefreshToken) { "no new refresh token" }
}
