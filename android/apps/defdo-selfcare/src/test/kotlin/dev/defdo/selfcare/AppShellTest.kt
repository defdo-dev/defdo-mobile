package dev.defdo.selfcare

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.selfcare.bootstrap.BootstrapClient
import dev.defdo.selfcare.bootstrap.BootstrapResult
import dev.defdo.selfcare.bootstrap.HttpResponse
import dev.defdo.selfcare.session.AuthSessionCoordinator
import dev.defdo.selfcare.startup.AppState
import dev.defdo.selfcare.startup.AppStartupCoordinator
import dev.defdo.selfcare.theme.EmbeddedTheme
import dev.defdo.selfcare.theme.InMemoryThemeCache
import dev.defdo.selfcare.theme.ThemeClient
import dev.defdo.selfcare.theme.ThemeRepository
import dev.defdo.selfcare.theme.ThemeSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AppShellTest {

    private val bootstrapUrl = "https://api.test/mobile/bootstrap"
    private val themeUrl = "https://api.test/mobile/theme"

    private fun ok(body: String, headers: Map<String, String> = emptyMap()) =
        HttpResponse(200, headers, body)

    private fun coordinator(
        token: String?,
        http: FakeHttpClient,
        devDiagnostics: Boolean = false
    ): Pair<AppStartupCoordinator, FakeSessionProvider> {
        val provider = FakeSessionProvider(token)
        val session = AuthSessionCoordinator(provider)
        val bootstrap = BootstrapClient(http, bootstrapUrl)
        val themeRepo = ThemeRepository(ThemeClient(http, themeUrl), InMemoryThemeCache()) { 0L }
        return AppStartupCoordinator(session, bootstrap, themeRepo, devDiagnostics) to provider
    }

    // 1. no token starts signed-out
    @Test
    fun noTokenStartsSignedOut() {
        val http = FakeHttpClient { ok(Fixtures.readyBootstrap) }
        val (coord, _) = coordinator(token = null, http = http)
        assertEquals(AppState.SignedOut, coord.resolveAppState())
        assertTrue(http.requests.isEmpty(), "no token must not trigger bootstrap")
    }

    // 2. valid token triggers bootstrap
    @Test
    fun validTokenTriggersBootstrap() {
        val http = FakeHttpClient { ok(Fixtures.readyBootstrap) }
        val (coord, _) = coordinator(token = "tok", http = http)
        coord.resolveAppState()
        assertTrue(http.requests.any { it.url == bootstrapUrl && it.method == "POST" })
    }

    // 3. 401 bootstrap clears session
    @Test
    fun bootstrap401ClearsSession() {
        val http = FakeHttpClient { HttpResponse(401, emptyMap(), "") }
        val (coord, provider) = coordinator(token = "tok", http = http)
        assertEquals(AppState.SignedOut, coord.resolveAppState())
        assertEquals(1, provider.clearCount)
    }

    // 4. 403 bootstrap does not retry infinitely (single non-retryable error)
    @Test
    fun bootstrap403IsNotRetryable() {
        val http = FakeHttpClient { HttpResponse(403, emptyMap(), "") }
        val (coord, _) = coordinator(token = "tok", http = http)
        val state = coord.resolveAppState()
        assertTrue(state is AppState.Error && !state.retryable)
        assertEquals(1, http.requests.size, "403 must not loop bootstrap")
    }

    // 5. needs_line_linking maps to NeedsLineLinking
    @Test
    fun needsLineLinkingMaps() {
        val http = FakeHttpClient { ok(Fixtures.needsLineLinkingBootstrap) }
        val (coord, _) = coordinator(token = "tok", http = http)
        assertTrue(coord.resolveAppState() is AppState.NeedsLineLinking)
    }

    // 6. ready maps to ReadyHome
    @Test
    fun readyMaps() {
        val http = FakeHttpClient { ok(Fixtures.readyBootstrap) }
        val (coord, _) = coordinator(token = "tok", http = http)
        val state = coord.resolveAppState()
        assertTrue(state is AppState.ReadyHome)
        assertEquals("defdo-telecom", (state as AppState.ReadyHome).context.tenantCode)
    }

    // 7. malformed bootstrap response maps to safe error
    @Test
    fun malformedBootstrapMapsToError() {
        val http = FakeHttpClient { ok(Fixtures.malformedBootstrap) }
        val (coord, _) = coordinator(token = "tok", http = http)
        val state = coord.resolveAppState()
        assertTrue(state is AppState.Error)
    }

    // network error keeps signed-in state with retry
    @Test
    fun bootstrapNetworkErrorIsRetryable() {
        val http = FakeHttpClient { ok(Fixtures.readyBootstrap) }.apply { throwOnNext = true }
        val client = BootstrapClient(http, bootstrapUrl)
        val result = client.bootstrap("tok")
        assertTrue(result is BootstrapResult.NetworkError)
    }

    // 8. embedded fallback is available without network
    @Test
    fun embeddedFallbackAvailableWithoutNetwork() {
        val http = FakeHttpClient { throw IllegalStateException("must not call network") }
        val repo = ThemeRepository(ThemeClient(http, themeUrl), InMemoryThemeCache())
        val applied = repo.localTheme(ThemeMode.LIGHT)
        assertEquals(ThemeSource.EMBEDDED, applied.source)
        assertEquals(EmbeddedTheme.light.tokens, applied.tokens.tokens)
        assertTrue(http.requests.isEmpty())
    }

    // 9. cached theme applies before network
    @Test
    fun cachedThemeAppliesBeforeNetwork() {
        val http = FakeHttpClient { throw IllegalStateException("must not call network") }
        val cache = InMemoryThemeCache()
        val repo = ThemeRepository(ThemeClient(http, themeUrl), cache)
        // Seed cache via a fresh fetch first using a separate client.
        val seedHttp = FakeHttpClient { ok(Fixtures.themeSuccess, mapOf("ETag" to "\"v1\"")) }
        ThemeRepository(ThemeClient(seedHttp, themeUrl), cache).refresh("tok", ThemeMode.LIGHT)

        val applied = repo.localTheme(ThemeMode.LIGHT)
        assertEquals(ThemeSource.CACHED, applied.source)
    }

    // 10. GET /mobile/theme 200 stores body + ETag
    @Test
    fun theme200StoresBodyAndEtag() {
        val http = FakeHttpClient { ok(Fixtures.themeSuccess, mapOf("ETag" to "\"v1\"")) }
        val cache = InMemoryThemeCache()
        ThemeRepository(ThemeClient(http, themeUrl), cache).refresh("tok", ThemeMode.LIGHT)
        val cached = cache.read(ThemeMode.LIGHT)
        assertNotNull(cached)
        assertEquals("\"v1\"", cached!!.etag)
        assertEquals("dev-theme-1", cached.themeVersion)
        assertEquals(1, cached.schemaVersion)
    }

    // 11. GET /mobile/theme 304 uses cached body
    @Test
    fun theme304UsesCachedBody() {
        val cache = InMemoryThemeCache()
        // Seed cache with a 200.
        ThemeRepository(
            ThemeClient(FakeHttpClient { ok(Fixtures.themeSuccess, mapOf("ETag" to "\"v1\"")) }, themeUrl),
            cache
        ).refresh("tok", ThemeMode.LIGHT)

        // Now a 304 with If-None-Match echoed.
        val condHttp = FakeHttpClient { req ->
            assertEquals("\"v1\"", req.headers["If-None-Match"])
            HttpResponse(304, emptyMap(), "")
        }
        val refresh = ThemeRepository(ThemeClient(condHttp, themeUrl), cache).refresh("tok", ThemeMode.LIGHT)
        assertEquals(dev.defdo.selfcare.theme.ThemeRefresh.UsedCache, refresh)
    }

    // 12. GET /mobile/theme 401 clears session (per app policy)
    @Test
    fun theme401ClearsSession() {
        val http = FakeHttpClient { HttpResponse(401, emptyMap(), "") }
        val (coord, provider) = coordinator(token = "tok", http = http)
        var signedOut = false
        val result = coord.refreshTheme(ThemeMode.LIGHT) { signedOut = true }
        assertNull(result)
        assertTrue(signedOut)
        assertEquals(1, provider.clearCount)
    }

    // 13. GET /mobile/theme 403 keeps fallback
    @Test
    fun theme403KeepsFallback() {
        val http = FakeHttpClient { HttpResponse(403, emptyMap(), "") }
        val repo = ThemeRepository(ThemeClient(http, themeUrl), InMemoryThemeCache())
        val refresh = repo.refresh("tok", ThemeMode.LIGHT)
        assertTrue(refresh is dev.defdo.selfcare.theme.ThemeRefresh.Diagnostic)
        // Local theme still resolves to embedded fallback.
        assertEquals(ThemeSource.EMBEDDED, repo.localTheme(ThemeMode.LIGHT).source)
    }

    // 14. GET /mobile/theme 404 keeps fallback
    @Test
    fun theme404KeepsFallback() {
        val http = FakeHttpClient { HttpResponse(404, emptyMap(), "") }
        val repo = ThemeRepository(ThemeClient(http, themeUrl), InMemoryThemeCache())
        val refresh = repo.refresh("tok", ThemeMode.LIGHT)
        assertTrue(refresh is dev.defdo.selfcare.theme.ThemeRefresh.Diagnostic)
        assertEquals(ThemeSource.EMBEDDED, repo.localTheme(ThemeMode.LIGHT).source)
    }

    // 14b. dev-only diagnostic surfacing: 403/404 only surface in dev builds
    @Test
    fun themeDiagnosticOnlyInDevBuilds() {
        val http = FakeHttpClient { HttpResponse(404, emptyMap(), "") }
        val (prodCoord, _) = coordinator(token = "tok", http = http, devDiagnostics = false)
        assertNull(prodCoord.refreshTheme(ThemeMode.LIGHT) {}?.diagnostic)

        val http2 = FakeHttpClient { HttpResponse(404, emptyMap(), "") }
        val (devCoord, _) = coordinator(token = "tok", http = http2, devDiagnostics = true)
        assertNotNull(devCoord.refreshTheme(ThemeMode.LIGHT) {}?.diagnostic)
    }

    // 15. invalid cached/remote theme is discarded
    @Test
    fun invalidThemeIsDiscarded() {
        val cache = InMemoryThemeCache()
        // wrong schema version
        ThemeRepository(ThemeClient(FakeHttpClient { ok(Fixtures.themeWrongSchema) }, themeUrl), cache)
            .refresh("tok", ThemeMode.LIGHT)
        assertNull(cache.read(ThemeMode.LIGHT), "wrong schema_version must not be cached")

        // invalid color
        ThemeRepository(ThemeClient(FakeHttpClient { ok(Fixtures.themeInvalidColor) }, themeUrl), cache)
            .refresh("tok", ThemeMode.LIGHT)
        assertNull(cache.read(ThemeMode.LIGHT), "invalid color must not be cached")
    }

    // 16. app does not send brand/app/theme/tenant as authority
    @Test
    fun doesNotSendBrandAppTenantAsAuthority() {
        val http = FakeHttpClient { req ->
            if (req.url.contains("bootstrap")) ok(Fixtures.readyBootstrap)
            else ok(Fixtures.themeSuccess, mapOf("ETag" to "\"v1\""))
        }
        val (coord, _) = coordinator(token = "tok", http = http)
        coord.resolveAppState()
        coord.refreshTheme(ThemeMode.LIGHT) {}

        val forbidden = listOf("brand_code", "brand_key", "app_key", "theme_code", "tenant")
        for (req in http.requests) {
            val haystack = (req.url + " " + req.headers.entries.joinToString { "${it.key}=${it.value}" } +
                " " + (req.body ?: "")).lowercase()
            for (term in forbidden) {
                assertFalse(haystack.contains(term), "request must not carry '$term' as authority: ${req.url}")
            }
        }
    }

    // 17. app does not log tokens (clients only place token in Authorization header)
    @Test
    fun tokensOnlyInAuthorizationHeaderNeverInUrlOrBody() {
        val http = FakeHttpClient { req ->
            if (req.url.contains("bootstrap")) ok(Fixtures.readyBootstrap)
            else ok(Fixtures.themeSuccess)
        }
        val (coord, _) = coordinator(token = "secret-token", http = http)
        coord.resolveAppState()
        coord.refreshTheme(ThemeMode.LIGHT) {}

        for (req in http.requests) {
            assertFalse(req.url.contains("secret-token"), "token must not appear in URL")
            assertFalse((req.body ?: "").contains("secret-token"), "token must not appear in body")
            assertEquals("Bearer secret-token", req.headers["Authorization"])
        }
    }
}
