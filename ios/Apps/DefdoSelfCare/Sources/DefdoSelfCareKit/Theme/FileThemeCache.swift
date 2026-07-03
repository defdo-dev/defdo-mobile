import Foundation
import DefdoThemeMobile

/// Disk-backed last-known-good theme cache. One file per mode under a caches
/// directory. Persists: theme body, ETag, fetched_at, schema_version,
/// theme_version. On read, the body is re-parsed and re-validated via
/// ThemeCodec; invalid cache is discarded and deleted so the app falls back to
/// embedded.
public final class FileThemeCache: AppThemeCache, @unchecked Sendable {
    private let directory: URL
    private let fileManager = FileManager.default

    public init(directory: URL) {
        self.directory = directory
    }

    private func fileURL(for mode: ThemeMode) -> URL {
        directory.appendingPathComponent("theme_\(mode.rawValue).json")
    }

    public func read(mode: ThemeMode) -> CachedTheme? {
        let url = fileURL(for: mode)
        guard let data = try? Data(contentsOf: url),
              let envelope = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let body = envelope["body"] as? String else {
            try? fileManager.removeItem(at: url)
            return nil
        }
        // Re-validate the persisted body before trusting it.
        guard let tokens = ThemeCodec.parse(body), tokens.mode == mode else {
            try? fileManager.removeItem(at: url)
            return nil
        }
        return CachedTheme(
            body: body,
            etag: envelope["etag"] as? String,
            fetchedAt: envelope["fetched_at"] as? Int ?? 0,
            schemaVersion: tokens.schemaVersion,
            themeVersion: tokens.themeVersion,
            tokens: tokens
        )
    }

    public func write(_ cached: CachedTheme) {
        try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        let envelope: [String: Any] = [
            "body": cached.body,
            "etag": cached.etag as Any,
            "fetched_at": cached.fetchedAt,
            "schema_version": cached.schemaVersion,
            "theme_version": cached.themeVersion
        ]
        if let data = try? JSONSerialization.data(withJSONObject: envelope) {
            try? data.write(to: fileURL(for: cached.tokens.mode))
        }
    }

    public func clear() {
        guard let files = try? fileManager.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil) else {
            return
        }
        for file in files { try? fileManager.removeItem(at: file) }
    }
}
