import Foundation
import DefdoSelfCareKit

#if os(iOS)
import SwiftUI
import AuthenticationServices
import DefdoAuthMobile
import DefdoThemeMobile

@main
struct DefdoSelfCareApp: App {
    @StateObject private var model: AppModel
    private let composition: Composition

    init() {
        let composition = Composition()
        self.composition = composition
        _model = StateObject(wrappedValue: composition.makeModel())
    }

    var body: some Scene {
        WindowGroup {
            RootView(model: model, onLogin: composition.startLogin)
                .onOpenURL { url in composition.handleCallback(url: url, model: model) }
                .onAppear { model.start() }
        }
    }
}

/// Composition root: builds the dependency graph from AppConfig and the
/// reusable auth/theme libraries. No production credentials are hardcoded.
@MainActor
final class Composition {
    let config: AppConfig
    let authClient: DefdoAuthMobileClient
    let coordinator: AppStartupCoordinator
    private var currentState = ""

    private let mode: ThemeMode = .light

    init() {
        config = AppConfig.fromBundle(Bundle.main.infoDictionary ?? [:])

        let authConfig = AuthConfig(
            clientID: config.clientID,
            discoveryURL: URL(string: config.discoveryURL) ?? URL(string: "https://invalid.example")!,
            redirectURI: config.redirectURI,
            scopes: config.scopes
        )
        let discovery = OAuthDiscoveryDocument(
            issuer: config.issuer,
            authorizationEndpoint: "\(config.issuer)/oauth/authorize",
            tokenEndpoint: "\(config.issuer)/oauth/token",
            revocationEndpoint: "\(config.issuer)/oauth/revoke"
        )
        let callbackScheme = config.redirectURI.split(separator: ":").first.map(String.init) ?? "https"
        let browser = ASWebAuthenticationSessionBrowserAdapter(
            callbackScheme: callbackScheme,
            contextProvider: PresentationContextProvider()
        )
        let store = SecureTokenStore(storage: KeychainSecureStorageAdapter())
        let client = DefdoAuthMobileClientImpl(
            config: authConfig,
            discovery: discovery,
            browserAdapter: browser,
            tokenStore: store,
            tokenTransport: URLSessionTokenHttpTransport()
        )
        authClient = client

        let session = AuthSessionCoordinator(provider: AuthClientSessionProvider(client: client))
        let http = URLSessionHTTPClient()
        let bootstrap = BootstrapClient(http: http, endpoint: config.bootstrapEndpoint)
        let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("DefdoTheme")
        let themeCache = FileThemeCache(directory: cacheDir)
        let themeClient = ThemeClient(http: http, endpoint: config.themeEndpoint)
        let themeRepo = AppThemeRepository(client: themeClient, cache: themeCache)

        #if DEBUG
        let devDiagnostics = true
        #else
        let devDiagnostics = false
        #endif

        coordinator = AppStartupCoordinator(
            session: session,
            bootstrapClient: bootstrap,
            themeRepository: themeRepo,
            devDiagnostics: devDiagnostics
        )
    }

    func makeModel() -> AppModel {
        AppModel(coordinator: coordinator, mode: mode)
    }

    func startLogin() {
        guard config.isConfigured else { return }
        currentState = UUID().uuidString
        let authConfig = AuthConfig(
            clientID: config.clientID,
            discoveryURL: URL(string: config.discoveryURL) ?? URL(string: "https://invalid.example")!,
            redirectURI: config.redirectURI,
            scopes: config.scopes
        )
        let request = LoginRequest(
            config: authConfig,
            state: currentState,
            nonce: UUID().uuidString,
            codeVerifier: PKCE.generateVerifier()
        )
        Task { _ = await authClient.startLogin(request) }
    }

    func handleCallback(url: URL, model: AppModel) {
        Task {
            let result = await authClient.handleCallback(
                url,
                expectedState: currentState,
                expectedRedirectURI: config.redirectURI
            )
            if case .authenticated = result {
                model.onLoggedIn()
            }
        }
    }
}

private final class PresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        ASPresentationAnchor()
    }
}

#else

// Non-iOS (e.g. macOS toolchain build / CI) entry point so the executable
// target compiles. The real UI runs only on iOS.
@main
struct DefdoSelfCareMain {
    static func main() {
        let config = AppConfig.fromEnvironment()
        print("DefdoSelfCare shell — configured: \(config.isConfigured), env: \(config.environment)")
    }
}

#endif
