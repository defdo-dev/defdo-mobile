package dev.defdo.mobile.auth

import java.net.URI

object CallbackValidator {
    fun validate(callbackUrl: String, expectedState: String, expectedRedirectUri: String): Result<String> {
        val callbackUri = try {
            URI(callbackUrl)
        } catch (_: Exception) {
            return Result.failure(AuthError.InvalidCallback("invalid callback URL"))
        }
        val expectedUri = try {
            URI(expectedRedirectUri)
        } catch (_: Exception) {
            return Result.failure(AuthError.InvalidCallback("invalid expected redirect URI"))
        }

        if (callbackUri.scheme != expectedUri.scheme) {
            return Result.failure(AuthError.InvalidCallback("wrong redirect URI scheme"))
        }
        if (callbackUri.host != expectedUri.host) {
            return Result.failure(AuthError.InvalidCallback("wrong redirect URI host"))
        }
        if (callbackUri.port != expectedUri.port) {
            return Result.failure(AuthError.InvalidCallback("wrong redirect URI port"))
        }
        val callbackPath = callbackUri.rawPath ?: ""
        val expectedPath = expectedUri.rawPath ?: ""
        if (callbackPath != expectedPath) {
            return Result.failure(AuthError.InvalidCallback("wrong redirect URI path"))
        }

        val query = callbackUri.rawQuery ?: return Result.failure(AuthError.InvalidCallback("missing query"))
        val pairs = query.split("&").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq == -1) null
            else decode(part.substring(0, eq)) to decode(part.substring(eq + 1))
        }

        val paramNames = pairs.map { it.first }
        if (paramNames.size != paramNames.distinct().size) {
            return Result.failure(AuthError.InvalidCallback("suspicious duplicated params"))
        }

        val params = pairs.toMap()

        params["error"]?.let { return Result.failure(AuthError.InvalidCallback("oauth error callback: $it")) }
        val code = params["code"] ?: return Result.failure(AuthError.InvalidCallback("missing code"))
        val state = params["state"] ?: return Result.failure(AuthError.InvalidCallback("missing state"))
        if (state != expectedState) return Result.failure(AuthError.InvalidCallback("mismatched state"))

        return Result.success(code)
    }

    private fun decode(value: String): String = java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
}
