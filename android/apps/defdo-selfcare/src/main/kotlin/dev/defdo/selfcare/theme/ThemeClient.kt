package dev.defdo.selfcare.theme

import dev.defdo.mobile.theme.ThemeTokens
import dev.defdo.selfcare.bootstrap.HttpClient

/**
 * Outcome of a GET /mobile/theme call. Keeps fallback semantics explicit so the
 * repository never blocks startup or crashes on theme failures.
 */
sealed class ThemeFetchResult {
    /** 200 — fresh theme + ETag to cache and apply. */
    data class Fresh(val tokens: ThemeTokens, val body: String, val etag: String?) : ThemeFetchResult()

    /** 304 — caller should use cached body. */
    object NotModified : ThemeFetchResult()

    /** 401 — clear auth session / return to signed out. */
    object Unauthorized : ThemeFetchResult()

    /** 403 — keep embedded/last-known-good; diagnostic only in dev builds. */
    data class Forbidden(val diagnostic: String) : ThemeFetchResult()

    /** 404 — keep embedded/last-known-good; diagnostic only in dev builds. */
    data class NotFound(val diagnostic: String) : ThemeFetchResult()

    /** Network/transport failure or invalid body — keep fallback. */
    data class Unavailable(val diagnostic: String) : ThemeFetchResult()
}

/**
 * Client for GET /mobile/theme.
 *
 * Sends ONLY the bearer token and an optional If-None-Match. It does NOT send
 * brand_code, brand_key, app_key, theme_code, or tenant as authority. The
 * backend already knows the brand/app/theme from the OAuth client behind the
 * token.
 */
class ThemeClient(
    private val http: HttpClient,
    private val endpoint: String
) {
    fun fetch(accessToken: String, etag: String?): ThemeFetchResult {
        val headers = buildMap {
            put("Authorization", "Bearer $accessToken")
            put("Accept", "application/json")
            if (!etag.isNullOrBlank()) put("If-None-Match", etag)
        }

        val response = try {
            http.request("GET", endpoint, headers, null)
        } catch (e: Exception) {
            return ThemeFetchResult.Unavailable(e.message ?: "transport failure")
        }

        return when (response.status) {
            200 -> {
                val tokens = ThemeCodec.parse(response.body)
                    ?: return ThemeFetchResult.Unavailable("invalid theme body")
                ThemeFetchResult.Fresh(tokens, response.body, response.header("ETag"))
            }
            304 -> ThemeFetchResult.NotModified
            401 -> ThemeFetchResult.Unauthorized
            403 -> ThemeFetchResult.Forbidden("theme forbidden (403)")
            404 -> ThemeFetchResult.NotFound("theme not found (404)")
            else -> ThemeFetchResult.Unavailable("unexpected status ${response.status}")
        }
    }
}
