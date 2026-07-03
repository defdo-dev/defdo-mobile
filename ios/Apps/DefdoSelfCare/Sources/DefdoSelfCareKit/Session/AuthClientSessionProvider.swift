import Foundation
import DefdoAuthMobile

/// Real SessionProvider backed by the auth library. Tokens are read only from
/// the auth client's SecureTokenStore (Keychain). No tokens are ever read from
/// or written to UserDefaults here.
public struct AuthClientSessionProvider: SessionProvider {
    private let client: DefdoAuthMobileClient

    public init(client: DefdoAuthMobileClient) {
        self.client = client
    }

    public func currentAccessToken() async -> String? {
        await client.currentSession()?.accessToken
    }

    public func clear() async {
        guard let session = await client.currentSession() else { return }
        // revoke() also clears local secure storage.
        _ = await client.revoke(session)
    }
}
