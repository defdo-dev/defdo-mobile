import Foundation

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}

/// Build-time / environment configuration for the SelfCare app shell.
///
/// Important: `clientID` is the only brand/app authority the app asserts, and it
/// is asserted to defdo_auth during the OAuth flow — not to the product backend.
/// The app never sends brand_code, brand_key, app_key, theme_code, or tenant as
/// authority to /mobile/bootstrap or /mobile/theme. The backend derives that
/// context from the OAuth client + the AccessContext it returns.
///
/// No production credentials are hardcoded here.
public struct AppConfig: Sendable, Equatable {
    public let issuer: String
    public let discoveryURL: String
    public let clientID: String
    public let redirectURI: String
    public let scopes: [String]
    public let bootstrapEndpoint: String
    public let themeEndpoint: String
    public let environment: String

    public init(
        issuer: String,
        discoveryURL: String,
        clientID: String,
        redirectURI: String,
        scopes: [String],
        bootstrapEndpoint: String,
        themeEndpoint: String,
        environment: String
    ) {
        self.issuer = issuer
        self.discoveryURL = discoveryURL
        self.clientID = clientID
        self.redirectURI = redirectURI
        self.scopes = scopes
        self.bootstrapEndpoint = bootstrapEndpoint
        self.themeEndpoint = themeEndpoint
        self.environment = environment
    }

    public var isConfigured: Bool { !issuer.isEmpty && !clientID.isEmpty }

    /// True when the configured redirect URI uses a custom scheme. Verified
    /// Universal Links / HTTPS are preferred for production.
    public var usesCustomScheme: Bool { !redirectURI.hasPrefix("https://") }

    /// Helper used by tests and Info.plist sanity checks to reconstruct a
    /// redirect URI from its components so the manifest stays consistent with
    /// runtime config.
    public func redirectURI(scheme: String, host: String, path: String) -> String {
        let normalizedPath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        return "\(scheme)://\(host)/\(normalizedPath)"
    }

    /// Resolves config from an explicit dictionary. Used by the Xcode target's
    /// Info.plist or test injection so the running app does not depend on
    /// process environment variables being present on device.
    public static func fromBundle(
        _ info: [String: Any],
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> AppConfig {
        func string(_ key: String) -> String? {
            (info[key] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines)
                .nilIfEmpty
                ?? environment[key]?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        }

        let issuer = string("DEFDO_DEV_ISSUER") ?? ""
        let discoveryURL = string("DEFDO_DEV_DISCOVERY_URL")
            ?? (issuer.isEmpty ? "" : "\(issuer)/.well-known/openid-configuration")
        let clientID = string("DEFDO_DEV_CLIENT_ID") ?? "defdo-telecom-mobile-dev"
        let redirectURI = string("DEFDO_DEV_REDIRECT_URI")
            ?? "https://login.defdo-telecom.example/mobile/oauth/callback"
        let scopes = (string("DEFDO_DEV_SCOPES") ?? "openid profile offline_access")
            .split(separator: " ").map(String.init)
            .filter { !$0.isEmpty }
        let backendBase = string("DEFDO_BACKEND_BASE_URL") ?? "https://api.defdo.example"

        return AppConfig(
            issuer: issuer,
            discoveryURL: discoveryURL,
            clientID: clientID,
            redirectURI: redirectURI,
            scopes: scopes,
            bootstrapEndpoint: "\(backendBase)/mobile/bootstrap",
            themeEndpoint: "\(backendBase)/mobile/theme",
            environment: string("DEFDO_ENVIRONMENT") ?? "dev"
        )
    }

    /// Resolves config from environment variables (dev harness style) with
    /// non-secret defaults. Production builds inject these through the brand
    /// manifest / build config, never as hardcoded credentials.
    public static func fromEnvironment(
        _ env: [String: String] = ProcessInfo.processInfo.environment
    ) -> AppConfig {
        let issuer = env["DEFDO_DEV_ISSUER"] ?? ""
        let discoveryURL = env["DEFDO_DEV_DISCOVERY_URL"]
            ?? (issuer.isEmpty ? "" : "\(issuer)/.well-known/openid-configuration")
        let clientID = env["DEFDO_DEV_CLIENT_ID"] ?? "defdo-telecom-mobile-dev"
        let redirectURI = env["DEFDO_DEV_REDIRECT_URI"]
            ?? "https://login.defdo-telecom.example/mobile/oauth/callback"
        let scopes = (env["DEFDO_DEV_SCOPES"] ?? "openid profile offline_access")
            .split(separator: " ").map(String.init)
        let backendBase = env["DEFDO_BACKEND_BASE_URL"] ?? "https://api.defdo.example"

        return AppConfig(
            issuer: issuer,
            discoveryURL: discoveryURL,
            clientID: clientID,
            redirectURI: redirectURI,
            scopes: scopes,
            bootstrapEndpoint: "\(backendBase)/mobile/bootstrap",
            themeEndpoint: "\(backendBase)/mobile/theme",
            environment: env["DEFDO_ENVIRONMENT"] ?? "dev"
        )
    }
}
