import XCTest
import DefdoThemeMobile
@testable import DefdoSelfCareKit

/// Free helper so responder closures stay @Sendable (no capture of the
/// XCTestCase instance).
private func ok(_ body: String, headers: [String: String] = [:]) -> HTTPResponse {
    HTTPResponse(status: 200, headers: headers, body: body)
}

final class AppShellTests: XCTestCase {
    private let bootstrapURL = "https://api.test/mobile/bootstrap"
    private let themeURL = "https://api.test/mobile/theme"

    private func makeCoordinator(
        token: String?,
        http: FakeHTTPClient,
        devDiagnostics: Bool = false
    ) -> (AppStartupCoordinator, FakeSessionProvider) {
        let provider = FakeSessionProvider(token: token)
        let session = AuthSessionCoordinator(provider: provider)
        let bootstrap = BootstrapClient(http: http, endpoint: bootstrapURL)
        let repo = AppThemeRepository(client: ThemeClient(http: http, endpoint: themeURL), cache: InMemoryThemeCache(), now: { 0 })
        return (AppStartupCoordinator(session: session, bootstrapClient: bootstrap, themeRepository: repo, devDiagnostics: devDiagnostics), provider)
    }

    // 1. no token starts signed-out
    func testNoTokenStartsSignedOut() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.readyBootstrap) }
        let (coord, _) = makeCoordinator(token: nil, http: http)
        let state = await coord.resolveAppState()
        XCTAssertEqual(state, .signedOut)
        let requests = await http.requests
        XCTAssertTrue(requests.isEmpty)
    }

    // 2. valid token triggers bootstrap
    func testValidTokenTriggersBootstrap() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.readyBootstrap) }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        _ = await coord.resolveAppState()
        let requests = await http.requests
        XCTAssertTrue(requests.contains { $0.url == bootstrapURL && $0.method == "POST" })
    }

    // 3. 401 bootstrap clears session
    func testBootstrap401ClearsSession() async {
        let http = FakeHTTPClient { _ in HTTPResponse(status: 401, headers: [:], body: "") }
        let (coord, provider) = makeCoordinator(token: "tok", http: http)
        let state = await coord.resolveAppState()
        XCTAssertEqual(state, .signedOut)
        let clearCount = await provider.clearCount
        XCTAssertEqual(clearCount, 1)
    }

    // 4. 403 bootstrap does not retry infinitely
    func testBootstrap403NotRetryable() async {
        let http = FakeHTTPClient { _ in HTTPResponse(status: 403, headers: [:], body: "") }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        let state = await coord.resolveAppState()
        if case let .error(_, retryable) = state {
            XCTAssertFalse(retryable)
        } else {
            XCTFail("expected error state")
        }
        let requests = await http.requests
        XCTAssertEqual(requests.count, 1)
    }

    // 5. needs_line_linking maps
    func testNeedsLineLinkingMaps() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.needsLineLinkingBootstrap) }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        let state = await coord.resolveAppState()
        if case .needsLineLinking = state {} else { XCTFail("expected needsLineLinking") }
    }

    // 6. ready maps
    func testReadyMaps() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.readyBootstrap) }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        let state = await coord.resolveAppState()
        if case let .readyHome(context) = state {
            XCTAssertEqual(context.tenantCode, "defdo-telecom")
        } else {
            XCTFail("expected readyHome")
        }
    }

    // 7. malformed bootstrap maps to error
    func testMalformedBootstrapMapsToError() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.malformedBootstrap) }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        let state = await coord.resolveAppState()
        if case .error = state {} else { XCTFail("expected error") }
    }

    // network error keeps signed-in with retry
    func testBootstrapNetworkErrorRetryable() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.readyBootstrap) }
        await http.setThrowOnNext()
        let client = BootstrapClient(http: http, endpoint: bootstrapURL)
        let result = await client.bootstrap(accessToken: "tok")
        if case .networkError = result {} else { XCTFail("expected networkError") }
    }

    // 8. embedded fallback without network
    func testEmbeddedFallbackWithoutNetwork() async {
        let http = FakeHTTPClient { _ in XCTFail("must not call network"); return HTTPResponse(status: 0, headers: [:], body: "") }
        let repo = AppThemeRepository(client: ThemeClient(http: http, endpoint: themeURL), cache: InMemoryThemeCache())
        let applied = repo.localTheme(mode: .light)
        XCTAssertEqual(applied.source, .embedded)
        XCTAssertEqual(applied.tokens.tokens, EmbeddedTheme.light.tokens)
        let requests = await http.requests
        XCTAssertTrue(requests.isEmpty)
    }

    // 9. cached theme applies before network
    func testCachedThemeAppliesBeforeNetwork() async {
        let cache = InMemoryThemeCache()
        let seedHTTP = FakeHTTPClient { _ in ok(Fixtures.themeSuccess, headers: ["ETag": "\"v1\""]) }
        _ = await AppThemeRepository(client: ThemeClient(http: seedHTTP, endpoint: themeURL), cache: cache)
            .refresh(accessToken: "tok", mode: .light)

        let offlineHTTP = FakeHTTPClient { _ in XCTFail("must not call network"); return HTTPResponse(status: 0, headers: [:], body: "") }
        let repo = AppThemeRepository(client: ThemeClient(http: offlineHTTP, endpoint: themeURL), cache: cache)
        XCTAssertEqual(repo.localTheme(mode: .light).source, .cached)
    }

    // 10. theme 200 stores body + ETag
    func testTheme200StoresBodyAndEtag() async {
        let http = FakeHTTPClient { _ in ok(Fixtures.themeSuccess, headers: ["ETag": "\"v1\""]) }
        let cache = InMemoryThemeCache()
        _ = await AppThemeRepository(client: ThemeClient(http: http, endpoint: themeURL), cache: cache)
            .refresh(accessToken: "tok", mode: .light)
        let cached = cache.read(mode: .light)
        XCTAssertNotNil(cached)
        XCTAssertEqual(cached?.etag, "\"v1\"")
        XCTAssertEqual(cached?.themeVersion, "dev-theme-1")
        XCTAssertEqual(cached?.schemaVersion, 1)
    }

    // 11. theme 304 uses cached body
    func testTheme304UsesCachedBody() async {
        let cache = InMemoryThemeCache()
        _ = await AppThemeRepository(
            client: ThemeClient(http: FakeHTTPClient { _ in ok(Fixtures.themeSuccess, headers: ["ETag": "\"v1\""]) }, endpoint: themeURL),
            cache: cache
        ).refresh(accessToken: "tok", mode: .light)

        let condHTTP = FakeHTTPClient { req in
            XCTAssertEqual(req.headers["If-None-Match"], "\"v1\"")
            return HTTPResponse(status: 304, headers: [:], body: "")
        }
        let refresh = await AppThemeRepository(client: ThemeClient(http: condHTTP, endpoint: themeURL), cache: cache)
            .refresh(accessToken: "tok", mode: .light)
        XCTAssertEqual(refresh, .usedCache)
    }

    // 12. theme 401 clears session
    func testTheme401ClearsSession() async {
        let http = FakeHTTPClient { _ in HTTPResponse(status: 401, headers: [:], body: "") }
        let (coord, provider) = makeCoordinator(token: "tok", http: http)
        actor Flag { var v = false; func set() { v = true } }
        let flag = Flag()
        let result = await coord.refreshTheme(mode: .light) { await flag.set() }
        XCTAssertNil(result)
        let wasSet = await flag.v
        XCTAssertTrue(wasSet)
        let clearCount = await provider.clearCount
        XCTAssertEqual(clearCount, 1)
    }

    // 13. theme 403 keeps fallback
    func testTheme403KeepsFallback() async {
        let http = FakeHTTPClient { _ in HTTPResponse(status: 403, headers: [:], body: "") }
        let repo = AppThemeRepository(client: ThemeClient(http: http, endpoint: themeURL), cache: InMemoryThemeCache())
        let refresh = await repo.refresh(accessToken: "tok", mode: .light)
        if case .diagnostic = refresh {} else { XCTFail("expected diagnostic") }
        XCTAssertEqual(repo.localTheme(mode: .light).source, .embedded)
    }

    // 14. theme 404 keeps fallback
    func testTheme404KeepsFallback() async {
        let http = FakeHTTPClient { _ in HTTPResponse(status: 404, headers: [:], body: "") }
        let repo = AppThemeRepository(client: ThemeClient(http: http, endpoint: themeURL), cache: InMemoryThemeCache())
        let refresh = await repo.refresh(accessToken: "tok", mode: .light)
        if case .diagnostic = refresh {} else { XCTFail("expected diagnostic") }
        XCTAssertEqual(repo.localTheme(mode: .light).source, .embedded)
    }

    // 14b. diagnostic only surfaces in dev builds
    func testThemeDiagnosticOnlyInDevBuilds() async {
        let prodHTTP = FakeHTTPClient { _ in HTTPResponse(status: 404, headers: [:], body: "") }
        let (prodCoord, _) = makeCoordinator(token: "tok", http: prodHTTP, devDiagnostics: false)
        let prodResult = await prodCoord.refreshTheme(mode: .light) {}
        XCTAssertNil(prodResult?.diagnostic)

        let devHTTP = FakeHTTPClient { _ in HTTPResponse(status: 404, headers: [:], body: "") }
        let (devCoord, _) = makeCoordinator(token: "tok", http: devHTTP, devDiagnostics: true)
        let devResult = await devCoord.refreshTheme(mode: .light) {}
        XCTAssertNotNil(devResult?.diagnostic)
    }

    // 15. invalid theme is discarded
    func testInvalidThemeIsDiscarded() async {
        let cache = InMemoryThemeCache()
        _ = await AppThemeRepository(client: ThemeClient(http: FakeHTTPClient { _ in ok(Fixtures.themeWrongSchema) }, endpoint: themeURL), cache: cache)
            .refresh(accessToken: "tok", mode: .light)
        XCTAssertNil(cache.read(mode: .light), "wrong schema_version must not be cached")

        _ = await AppThemeRepository(client: ThemeClient(http: FakeHTTPClient { _ in ok(Fixtures.themeInvalidColor) }, endpoint: themeURL), cache: cache)
            .refresh(accessToken: "tok", mode: .light)
        XCTAssertNil(cache.read(mode: .light), "invalid color must not be cached")
    }

    // 16. no brand/app/theme/tenant sent as authority
    func testDoesNotSendBrandAppTenantAsAuthority() async {
        let http = FakeHTTPClient { req in
            req.url.contains("bootstrap") ? ok(Fixtures.readyBootstrap) : ok(Fixtures.themeSuccess, headers: ["ETag": "\"v1\""])
        }
        let (coord, _) = makeCoordinator(token: "tok", http: http)
        _ = await coord.resolveAppState()
        _ = await coord.refreshTheme(mode: .light) {}

        let forbidden = ["brand_code", "brand_key", "app_key", "theme_code", "tenant"]
        let requests = await http.requests
        for req in requests {
            let haystack = (req.url + " " + req.headers.map { "\($0)=\($1)" }.joined(separator: " ") + " " + (req.body ?? "")).lowercased()
            for term in forbidden {
                XCTAssertFalse(haystack.contains(term), "request must not carry '\(term)' as authority: \(req.url)")
            }
        }
    }

    // 17. tokens only in Authorization header
    func testTokensOnlyInAuthorizationHeader() async {
        let http = FakeHTTPClient { req in
            req.url.contains("bootstrap") ? ok(Fixtures.readyBootstrap) : ok(Fixtures.themeSuccess)
        }
        let (coord, _) = makeCoordinator(token: "secret-token", http: http)
        _ = await coord.resolveAppState()
        _ = await coord.refreshTheme(mode: .light) {}

        let requests = await http.requests
        for req in requests {
            XCTAssertFalse(req.url.contains("secret-token"))
            XCTAssertFalse((req.body ?? "").contains("secret-token"))
            XCTAssertEqual(req.headers["Authorization"], "Bearer secret-token")
        }
    }
}
