import Foundation
import DefdoThemeMobile

public enum ThemeSource: Sendable { case embedded, cached, remote }

public struct AppliedTheme: Sendable, Equatable {
    public let tokens: ThemeTokens
    public let source: ThemeSource
    public init(tokens: ThemeTokens, source: ThemeSource) {
        self.tokens = tokens
        self.source = source
    }
}

public enum ThemeRefresh: Sendable, Equatable {
    case applied(AppliedTheme)
    case usedCache
    case keptFallback
    case sessionInvalid
    /// Non-blocking diagnostic for dev builds only (403/404/unavailable).
    case diagnostic(String)
}

/// Layered theme resolution:
///   1. Embedded fallback (always available, no network)
///   2. Last-known-good cached runtime theme (validated before use)
///   3. Fresh runtime theme from GET /mobile/theme
///
/// Never blocks startup, never crashes on theme failure.
public struct AppThemeRepository: Sendable {
    private let client: ThemeClient
    private let cache: AppThemeCache
    private let now: @Sendable () -> Int

    public init(client: ThemeClient, cache: AppThemeCache, now: @escaping @Sendable () -> Int = { Int(Date().timeIntervalSince1970) }) {
        self.client = client
        self.cache = cache
        self.now = now
    }

    /// Steps 1+2: best theme available without (waiting on) the network.
    public func localTheme(mode: ThemeMode) -> AppliedTheme {
        if let cached = cache.read(mode: mode) {
            return AppliedTheme(tokens: cached.tokens, source: .cached)
        }
        return AppliedTheme(tokens: EmbeddedTheme.forMode(mode), source: .embedded)
    }

    /// Step 3: fetch fresh theme, applying cache/fallback policy.
    public func refresh(accessToken: String, mode: ThemeMode) async -> ThemeRefresh {
        let cached = cache.read(mode: mode)
        switch await client.fetch(accessToken: accessToken, etag: cached?.etag) {
        case let .fresh(tokens, body, etag):
            cache.write(CachedTheme(
                body: body,
                etag: etag,
                fetchedAt: now(),
                schemaVersion: tokens.schemaVersion,
                themeVersion: tokens.themeVersion,
                tokens: tokens
            ))
            return .applied(AppliedTheme(tokens: tokens, source: .remote))
        case .notModified:
            return cached != nil ? .usedCache : .keptFallback
        case .unauthorized:
            return .sessionInvalid
        case let .forbidden(message):
            return .diagnostic(message)
        case let .notFound(message):
            return .diagnostic(message)
        case let .unavailable(message):
            return .diagnostic(message)
        }
    }
}
