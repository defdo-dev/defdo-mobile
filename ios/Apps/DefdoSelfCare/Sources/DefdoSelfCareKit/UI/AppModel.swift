#if canImport(SwiftUI)
import SwiftUI
import DefdoThemeMobile

/// Drives the app shell. Bootstrap and theme run concurrently; theme refresh is
/// independent and never blocks navigation. Mirrors the Android AppViewModel.
@MainActor
public final class AppModel: ObservableObject {
    @Published public private(set) var appState: AppState = .launch
    @Published public private(set) var theme: AppliedTheme
    @Published public private(set) var diagnostic: String?

    private let coordinator: AppStartupCoordinator
    private let mode: ThemeMode

    public init(coordinator: AppStartupCoordinator, mode: ThemeMode) {
        self.coordinator = coordinator
        self.mode = mode
        self.theme = coordinator.initialTheme(mode: mode)
    }

    /// Steps 3-7: resolve state + refresh theme concurrently.
    public func start() {
        // Theme refresh (step 7) — independent, non-blocking.
        Task {
            let themeState = await coordinator.refreshTheme(mode: mode) { [weak self] in
                await MainActor.run { self?.appState = .signedOut }
            }
            if let themeState {
                self.theme = themeState.theme
                self.diagnostic = themeState.diagnostic
            }
        }

        // App state (steps 3-6).
        Task {
            appState = .bootstrapLoading
            appState = await coordinator.resolveAppState()
        }
    }

    public func onLoggedIn() { start() }
    public func retry() { start() }
}
#endif
