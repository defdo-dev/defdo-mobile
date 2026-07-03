package dev.defdo.mobile.auth

fun runCallbackValidatorTest() {
    val redirect = "https://login.defdo-telecom.example/mobile/oauth/callback"

    val success = CallbackValidator.validate("$redirect?code=abc&state=ok", "ok", redirect)
    check(success.getOrNull() == "abc") { "should extract code" }

    val missingCode = CallbackValidator.validate("$redirect?state=ok", "ok", redirect)
    check(missingCode.isFailure) { "should reject missing code" }
    check(missingCode.exceptionOrNull() is AuthError.InvalidCallback)

    val mismatchedState = CallbackValidator.validate("$redirect?code=abc&state=bad", "ok", redirect)
    check(mismatchedState.isFailure) { "should reject mismatched state" }
    check(mismatchedState.exceptionOrNull() is AuthError.InvalidCallback)

    val missingState = CallbackValidator.validate("$redirect?code=abc", "ok", redirect)
    check(missingState.isFailure) { "should reject missing state" }
    check(missingState.exceptionOrNull() is AuthError.InvalidCallback)

    val oauthError = CallbackValidator.validate("$redirect?error=access_denied&state=ok", "ok", redirect)
    check(oauthError.isFailure) { "should reject OAuth error callback" }
    check(oauthError.exceptionOrNull() is AuthError.InvalidCallback)

    val callbackEvil = CallbackValidator.validate(
        "https://login.defdo-telecom.example/mobile/oauth/callback-evil?code=abc&state=ok",
        "ok", redirect
    )
    check(callbackEvil.isFailure) { "should reject callback-evil path" }
    check(callbackEvil.exceptionOrNull() is AuthError.InvalidCallback)

    val differentHost = CallbackValidator.validate(
        "https://evil.example/mobile/oauth/callback?code=abc&state=ok",
        "ok", redirect
    )
    check(differentHost.isFailure) { "should reject different host" }
    check(differentHost.exceptionOrNull() is AuthError.InvalidCallback)

    val differentScheme = CallbackValidator.validate(
        "http://login.defdo-telecom.example/mobile/oauth/callback?code=abc&state=ok",
        "ok", redirect
    )
    check(differentScheme.isFailure) { "should reject different scheme" }

    val portRedirect = "https://login.defdo-telecom.example:8443/mobile/oauth/callback"
    val portMismatch = CallbackValidator.validate(
        "https://login.defdo-telecom.example:8080/mobile/oauth/callback?code=abc&state=ok",
        "ok", portRedirect
    )
    check(portMismatch.isFailure) { "should reject port mismatch" }

    val portMatch = CallbackValidator.validate(
        "https://login.defdo-telecom.example:8443/mobile/oauth/callback?code=abc&state=ok",
        "ok", portRedirect
    )
    check(portMatch.getOrNull() == "abc") { "should accept matching port" }

    val wrongUri = CallbackValidator.validate("https://other/callback?code=abc&state=ok", "ok", redirect)
    check(wrongUri.isFailure) { "should reject wrong redirect URI" }
    check(wrongUri.exceptionOrNull() is AuthError.InvalidCallback)

    val duplicatedParams = CallbackValidator.validate(
        "$redirect?code=abc&state=ok&state=evil", "ok", redirect
    )
    check(duplicatedParams.isFailure) { "should reject duplicated params" }
    check(duplicatedParams.exceptionOrNull() is AuthError.InvalidCallback)
}
