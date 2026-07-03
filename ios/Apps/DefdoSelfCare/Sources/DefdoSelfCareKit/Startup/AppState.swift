import DefdoThemeMobile

/// High-level app shell states the UI navigates between.
public enum AppState: Sendable, Equatable {
    case launch
    case signedOut
    case bootstrapLoading
    case needsLineLinking(AccessContext)
    case readyHome(AccessContext)
    /// Retryable error keeps the user signed in (e.g. network failure).
    case error(message: String, retryable: Bool)
}

/// Theme + diagnostic state, applied independently of `AppState`.
public struct ThemeState: Sendable, Equatable {
    public let theme: AppliedTheme
    public let diagnostic: String?
    public init(theme: AppliedTheme, diagnostic: String? = nil) {
        self.theme = theme
        self.diagnostic = diagnostic
    }
}
