import Foundation
import DefdoThemeMobile

/// Last-known-good cache envelope. Persists everything required to validate and
/// re-apply a previously fetched runtime theme, plus the ETag.
public struct CachedTheme: Sendable, Equatable {
    public let body: String
    public let etag: String?
    public let fetchedAt: Int
    public let schemaVersion: Int
    public let themeVersion: String
    public let tokens: ThemeTokens

    public init(body: String, etag: String?, fetchedAt: Int, schemaVersion: Int, themeVersion: String, tokens: ThemeTokens) {
        self.body = body
        self.etag = etag
        self.fetchedAt = fetchedAt
        self.schemaVersion = schemaVersion
        self.themeVersion = themeVersion
        self.tokens = tokens
    }
}

/// App-local theme cache abstraction (file-backed in production).
public protocol AppThemeCache: Sendable {
    func read(mode: ThemeMode) -> CachedTheme?
    func write(_ cached: CachedTheme)
    func clear()
}

/// In-memory cache for tests.
public final class InMemoryThemeCache: AppThemeCache, @unchecked Sendable {
    private let lock = NSLock()
    private var store: [ThemeMode: CachedTheme] = [:]

    public init() {}

    public func read(mode: ThemeMode) -> CachedTheme? {
        lock.lock(); defer { lock.unlock() }
        return store[mode]
    }

    public func write(_ cached: CachedTheme) {
        lock.lock(); defer { lock.unlock() }
        store[cached.tokens.mode] = cached
    }

    public func clear() {
        lock.lock(); defer { lock.unlock() }
        store.removeAll()
    }
}

/// Parses + validates a runtime theme body. Returns nil when malformed or
/// invalid, so callers discard it and fall back.
public enum ThemeCodec {
    public static func parse(_ body: String) -> ThemeTokens? {
        guard let data = body.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        guard let schemaVersion = json["schema_version"] as? Int, schemaVersion == 1 else {
            return nil
        }
        guard let themeVersion = json["theme_version"] as? String,
              let modeString = json["mode"] as? String,
              let mode = ThemeMode(rawValue: modeString),
              let rawTokens = json["tokens"] as? [String: Any] else {
            return nil
        }
        var tokens: [String: String] = [:]
        for (key, value) in rawTokens {
            guard let str = value as? String else { return nil }
            tokens[key] = str
        }
        let parsed = ThemeTokens(
            schemaVersion: schemaVersion,
            themeVersion: themeVersion,
            mode: mode,
            tokens: tokens
        )
        if ThemeTokenValidator.validate(parsed) != nil { return nil }
        return parsed
    }
}
