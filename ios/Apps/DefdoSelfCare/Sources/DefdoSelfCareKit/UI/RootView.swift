#if canImport(SwiftUI)
import SwiftUI

public struct RootView: View {
    @ObservedObject private var model: AppModel
    private let onLogin: () -> Void

    public init(model: AppModel, onLogin: @escaping () -> Void) {
        self.model = model
        self.onLogin = onLogin
    }

    public var body: some View {
        let tokens = model.theme.tokens
        switch model.appState {
        case .launch:
            LaunchView(theme: tokens)
        case .signedOut:
            SignedOutView(theme: tokens, onLogin: onLogin)
        case .bootstrapLoading:
            BootstrapLoadingView(theme: tokens)
        case .needsLineLinking:
            NeedsLineLinkingView(theme: tokens)
        case let .readyHome(context):
            ReadyHomeView(theme: tokens, tenantLabel: context.tenantCode)
        case let .error(message, retryable):
            ErrorView(theme: tokens, message: message, onRetry: retryable ? { model.retry() } : nil)
        }
    }
}
#endif
