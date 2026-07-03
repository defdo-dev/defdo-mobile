import Foundation

/// Narrow session port over the auth library, so coordinators stay testable
/// without the full DefdoAuthMobileClient + platform adapters.
///
/// The real implementation (AuthClientSessionProvider) wraps
/// DefdoAuthMobile.DefdoAuthMobileClient and reads tokens only from Keychain —
/// never from UserDefaults.
public protocol SessionProvider: Sendable {
    func currentAccessToken() async -> String?
    func clear() async
}

/// Owns the signed-in / signed-out determination and session invalidation.
/// Token persistence itself lives in the auth library's SecureTokenStore backed
/// by the Keychain.
public struct AuthSessionCoordinator: Sendable {
    private let provider: SessionProvider

    public init(provider: SessionProvider) {
        self.provider = provider
    }

    public func hasSession() async -> Bool {
        await !(provider.currentAccessToken()?.isEmpty ?? true)
    }

    public func accessToken() async -> String? {
        await provider.currentAccessToken()
    }

    public func invalidate() async {
        await provider.clear()
    }
}
