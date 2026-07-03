import SwiftUI
import DefdoAuthMobile
import AuthenticationServices

@MainActor
final class DevAuthViewModel: ObservableObject {
    @Published var statusText = "Not configured"
    @Published var isConfigured = false

    private var client: DefdoAuthMobileClient?
    private var config: AuthConfig?
    private var discovery: OAuthDiscoveryDocument?
    private var currentState = ""

    init() {
        let issuer = ProcessInfo.processInfo.environment["DEFDO_DEV_ISSUER"]

        guard let issuer = issuer, !issuer.isEmpty else {
            statusText = "Not configured"
            return
        }

        let discoveryURLStr = ProcessInfo.processInfo.environment["DEFDO_DEV_DISCOVERY_URL"]
            ?? "\(issuer)/.well-known/openid-configuration"
        let clientID = ProcessInfo.processInfo.environment["DEFDO_DEV_CLIENT_ID"] ?? "dev-client"
        let redirectURI = ProcessInfo.processInfo.environment["DEFDO_DEV_REDIRECT_URI"] ?? "https://app.defdo.example/oauth/callback"
        let scopes = (ProcessInfo.processInfo.environment["DEFDO_DEV_SCOPES"] ?? "openid profile offline_access")
            .split(separator: " ").map(String.init)
        let callbackScheme = redirectURI.split(separator: ":").first.map(String.init) ?? "app.defdo.example"

        config = AuthConfig(
            clientID: clientID,
            discoveryURL: URL(string: discoveryURLStr)!,
            redirectURI: redirectURI,
            scopes: scopes
        )

        discovery = OAuthDiscoveryDocument(
            issuer: issuer,
            authorizationEndpoint: "\(issuer)/oauth/authorize",
            tokenEndpoint: "\(issuer)/oauth/token",
            revocationEndpoint: "\(issuer)/oauth/revoke"
        )

        if let error = discovery?.validate(issuer) {
            statusText = "discovery invalid: \(error)"
            return
        }

        let transport = URLSessionTokenHttpTransport()
        let storage = KeychainSecureStorageAdapter()
        let store = SecureTokenStore(storage: storage)

        client = DefdoAuthMobileClientImpl(
            config: config!,
            discovery: discovery!,
            browserAdapter: ASWebHarnessBrowserAdapter(callbackURLScheme: callbackScheme),
            tokenStore: store,
            tokenTransport: transport
        )

        isConfigured = true
        statusText = "configured: \(issuer)"
    }

    func startLogin() async {
        guard let client = client, let config = config else { return }
        currentState = UUID().uuidString
        let verifier = PKCE.generateVerifier()
        let nonce = UUID().uuidString
        let request = LoginRequest(config: config, state: currentState, nonce: nonce, codeVerifier: verifier)

        statusText = "login started"
        let result = await client.startLogin(request)
        if case .pendingBrowser = result {
            statusText = "login started (check browser)"
        }
    }

    func handleCallback(url: URL) async {
        guard let client = client, let config = config else { return }
        let result = await client.handleCallback(url, expectedState: currentState, expectedRedirectURI: config.redirectURI)
        switch result {
        case .authenticated:
            statusText = "authenticated"
        case .failed(let error):
            statusText = "login failed: \(String(describing: error))"
        case .loggedOut:
            statusText = "logged out"
        case .pendingBrowser:
            statusText = "pending browser"
        }
    }

    func refresh() async {
        guard let client = client else { return }
        guard let session = await client.currentSession() else {
            statusText = "no session"
            return
        }
        let result = await client.refresh(session)
        switch result {
        case .authenticated:
            statusText = "refresh succeeded"
        case .failed(let error):
            statusText = "refresh failed: \(String(describing: error))"
        default:
            statusText = "unexpected refresh result"
        }
    }

    func logout() async {
        guard let client = client else { return }
        guard let session = await client.currentSession() else {
            statusText = "no session"
            return
        }
        _ = await client.revoke(session)
        statusText = "logged out"
    }

    func clear() async {
        guard let client = client else { return }
        guard let session = await client.currentSession() else {
            statusText = "no session"
            return
        }
        _ = await client.revoke(session)
        statusText = "session cleared"
    }
}

@MainActor
private final class ASWebHarnessBrowserAdapter: BrowserAuthAdapter, Sendable {
    let callbackURLScheme: String
    private let contextProvider = HarnessContextProvider()

    init(callbackURLScheme: String) {
        self.callbackURLScheme = callbackURLScheme
    }

    func openAuthorizationURL(_ url: URL) async {
        await withCheckedContinuation { continuation in
            let session = ASWebAuthenticationSession(
                url: url,
                callbackURLScheme: callbackURLScheme
            ) { _, _ in
                continuation.resume()
            }
            session.presentationContextProvider = contextProvider
            session.start()
        }
    }
}

private final class HarnessContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding, @unchecked Sendable {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        ASPresentationAnchor()
    }
}
