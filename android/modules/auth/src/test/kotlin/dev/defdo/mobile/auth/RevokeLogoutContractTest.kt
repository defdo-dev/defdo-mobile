package dev.defdo.mobile.auth

fun runRevokeLogoutContractTest() {
    val successFixture = AuthTestFixtures.loadJson("revoke.success.fixture.json")
    check(successFixture["clear_local_session"] == true) { "revoke success must clear local session" }
    check(successFixture["status"] == "ok") { "revoke success status should be ok" }

    val failureFixture = AuthTestFixtures.loadJson("revoke.failure.fixture.json")
    check(failureFixture["clear_local_session"] == true) { "revoke failure must still clear local session" }
    check(failureFixture["status"] == "error") { "revoke failure status should be error" }

    val failureError = AuthErrorNormalizer.normalize(failureFixture["error"] as String)
    check(failureError is AuthError.Retryable) { "temporarily_unavailable should normalize to Retryable" }

    val success = RevokeResult.Success(clearLocalSession = true)
    check(success.clearLocalSession) { "success clearLocalSession must be true" }

    val failure = RevokeResult.Failure(
        error = AuthError.Retryable,
        clearLocalSession = true
    )
    check(failure.clearLocalSession) { "failure clearLocalSession must be true" }
    check(failure.error is AuthError.Retryable) { "failure error should be Retryable" }
}
