import DefdoThemeMobile

/// Orchestrates the app shell startup flow. Pure logic, no UIKit/SwiftUI, so it
/// is fully covered by unit tests. Mirrors the Android AppStartupCoordinator.
///
/// 403 from bootstrap is terminal (no infinite retry): it produces a
/// non-retryable error. Network errors keep the user signed in and are
/// retryable.
public struct AppStartupCoordinator: Sendable {
    private let session: AuthSessionCoordinator
    private let bootstrapClient: BootstrapClient
    private let themeRepository: AppThemeRepository
    private let devDiagnostics: Bool

    public init(
        session: AuthSessionCoordinator,
        bootstrapClient: BootstrapClient,
        themeRepository: AppThemeRepository,
        devDiagnostics: Bool = false
    ) {
        self.session = session
        self.bootstrapClient = bootstrapClient
        self.themeRepository = themeRepository
        self.devDiagnostics = devDiagnostics
    }

    /// Steps 1-2: theme available before any network.
    public func initialTheme(mode: ThemeMode) -> AppliedTheme {
        themeRepository.localTheme(mode: mode)
    }

    /// Steps 3-6: resolve the authoritative app state.
    public func resolveAppState() async -> AppState {
        guard let token = await session.accessToken(), !token.isEmpty else {
            return .signedOut
        }
        switch await bootstrapClient.bootstrap(accessToken: token) {
        case let .ready(context):
            return .readyHome(context)
        case let .needsLineLinking(context):
            return .needsLineLinking(context)
        case .unauthorized:
            await session.invalidate()
            return .signedOut
        case let .forbidden(message):
            return .error(message: message, retryable: false)
        case .networkError:
            return .error(message: "Can't reach the server. Check your connection.", retryable: true)
        case .malformedResponse:
            return .error(message: "Something went wrong. Please try again.", retryable: false)
        }
    }

    /// Step 7: refresh theme from network. Returns the theme/diagnostic to apply,
    /// or nil when nothing changes. A diagnostic is only meaningful in dev builds.
    ///
    /// - Parameter onSessionInvalid: invoked on theme 401 so the caller can sign out.
    public func refreshTheme(mode: ThemeMode, onSessionInvalid: @Sendable () async -> Void) async -> ThemeState? {
        guard let token = await session.accessToken() else { return nil }
        switch await themeRepository.refresh(accessToken: token, mode: mode) {
        case let .applied(theme):
            return ThemeState(theme: theme)
        case .usedCache, .keptFallback:
            return ThemeState(theme: themeRepository.localTheme(mode: mode))
        case .sessionInvalid:
            await session.invalidate()
            await onSessionInvalid()
            return nil
        case let .diagnostic(message):
            if devDiagnostics {
                return ThemeState(theme: themeRepository.localTheme(mode: mode), diagnostic: message)
            }
            return nil
        }
    }
}
