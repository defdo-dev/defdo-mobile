package dev.defdo.selfcare.theme

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.mobile.theme.ThemeTokens

/** Where the currently-applied theme came from. */
enum class ThemeSource { EMBEDDED, CACHED, REMOTE }

data class AppliedTheme(val tokens: ThemeTokens, val source: ThemeSource)

/** Result of a remote theme refresh, used by the startup coordinator. */
sealed class ThemeRefresh {
    data class Applied(val theme: AppliedTheme) : ThemeRefresh()
    object UsedCache : ThemeRefresh()
    object KeptFallback : ThemeRefresh()
    object SessionInvalid : ThemeRefresh()
    /** Non-blocking diagnostic for dev builds only (403/404/unavailable). */
    data class Diagnostic(val message: String) : ThemeRefresh()
}

/**
 * Layered theme resolution:
 *   1. Embedded fallback (always available, no network)
 *   2. Last-known-good cached runtime theme (validated before use)
 *   3. Fresh runtime theme from GET /mobile/theme
 *
 * Never blocks startup, never crashes on theme failure.
 */
class ThemeRepository(
    private val client: ThemeClient,
    private val cache: ThemeCache,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /** Step 1+2: best theme available without (waiting on) the network. */
    fun localTheme(mode: ThemeMode): AppliedTheme {
        val cached = cache.read(mode)
        if (cached != null) {
            return AppliedTheme(cached.tokens, ThemeSource.CACHED)
        }
        return AppliedTheme(EmbeddedTheme.forMode(mode), ThemeSource.EMBEDDED)
    }

    /** Step 3: fetch fresh theme, applying cache/fallback policy. */
    fun refresh(accessToken: String, mode: ThemeMode): ThemeRefresh {
        val cached = cache.read(mode)
        return when (val result = client.fetch(accessToken, cached?.etag)) {
            is ThemeFetchResult.Fresh -> {
                cache.write(
                    CachedTheme(
                        body = result.body,
                        etag = result.etag,
                        fetchedAt = clock(),
                        schemaVersion = result.tokens.schemaVersion,
                        themeVersion = result.tokens.themeVersion,
                        tokens = result.tokens
                    )
                )
                ThemeRefresh.Applied(AppliedTheme(result.tokens, ThemeSource.REMOTE))
            }
            is ThemeFetchResult.NotModified ->
                if (cached != null) ThemeRefresh.UsedCache else ThemeRefresh.KeptFallback
            is ThemeFetchResult.Unauthorized -> ThemeRefresh.SessionInvalid
            is ThemeFetchResult.Forbidden -> ThemeRefresh.Diagnostic(result.diagnostic)
            is ThemeFetchResult.NotFound -> ThemeRefresh.Diagnostic(result.diagnostic)
            is ThemeFetchResult.Unavailable -> ThemeRefresh.Diagnostic(result.diagnostic)
        }
    }
}
