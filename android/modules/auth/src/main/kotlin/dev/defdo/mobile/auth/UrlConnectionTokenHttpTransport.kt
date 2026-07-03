package dev.defdo.mobile.auth

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class UrlConnectionTokenHttpTransport : TokenHttpTransport {
    override fun exchangeCode(tokenEndpoint: String, params: Map<String, String>): Result<TokenResponse> {
        return postForm(tokenEndpoint, params).map { TokenResponse.parse(it) }
    }

    override fun refreshToken(tokenEndpoint: String, params: Map<String, String>): Result<RefreshResponse> {
        return postForm(tokenEndpoint, params).map { RefreshResponse.parse(it) }
    }

    override fun revokeToken(revocationEndpoint: String, params: Map<String, String>): Result<Unit> {
        return postForm(revocationEndpoint, params).map { }
    }

    private fun postForm(url: String, params: Map<String, String>): Result<Map<String, Any?>> {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            connection.outputStream.use { os ->
                os.write(formBody(params).toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val input = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = input?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                Result.success(parseSimpleJson(body))
            } else {
                Result.failure(mapHttpError(responseCode, body))
            }
        } catch (e: IOException) {
            Result.failure(AuthError.Retryable)
        } catch (e: Exception) {
            Result.failure(AuthError.OAuthError("transport_error"))
        }
    }

    private fun formBody(params: Map<String, String>): String {
        return params.entries.joinToString("\u0026") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun mapHttpError(code: Int, body: String): AuthError {
        return when (code) {
            in 500..599 -> AuthError.Retryable
            401, 403 -> AuthError.RequiresLogin
            else -> {
                val parsed = try { parseSimpleJson(body) } catch (_: Exception) { emptyMap() }
                val error = parsed["error"] as? String
                if (error != null) AuthErrorNormalizer.normalize(error) else AuthError.OAuthError("http_$code")
            }
        }
    }
}
