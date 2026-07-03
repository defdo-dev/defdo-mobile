package dev.defdo.selfcare

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.defdo.mobile.auth.AuthConfig
import dev.defdo.mobile.auth.DefdoAuthMobileClient
import dev.defdo.mobile.auth.DefdoAuthMobileClientImpl
import dev.defdo.mobile.auth.LoginRequest
import dev.defdo.mobile.auth.LoginResult
import dev.defdo.mobile.auth.OAuthDiscoveryDocument
import dev.defdo.mobile.auth.PKCE
import dev.defdo.mobile.auth.SecureTokenStore
import dev.defdo.mobile.auth.UrlConnectionTokenHttpTransport
import dev.defdo.mobile.auth.android.AndroidKeystoreSecureStorageAdapter
import dev.defdo.mobile.auth.android.AppLinkCallbackHandler
import dev.defdo.mobile.auth.android.ChromeCustomTabsBrowserAdapter
import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.selfcare.bootstrap.BootstrapClient
import dev.defdo.selfcare.bootstrap.UrlConnectionHttpClient
import dev.defdo.selfcare.config.AppConfig
import dev.defdo.selfcare.session.AuthClientSessionProvider
import dev.defdo.selfcare.session.AuthSessionCoordinator
import dev.defdo.selfcare.startup.AppStartupCoordinator
import dev.defdo.selfcare.startup.AppState
import dev.defdo.selfcare.theme.FileThemeCache
import dev.defdo.selfcare.theme.ThemeClient
import dev.defdo.selfcare.theme.ThemeRepository
import dev.defdo.selfcare.ui.AppViewModel
import dev.defdo.selfcare.ui.BootstrapLoadingScreen
import dev.defdo.selfcare.ui.ErrorScreen
import dev.defdo.selfcare.ui.LaunchScreen
import dev.defdo.selfcare.ui.NeedsLineLinkingScreen
import dev.defdo.selfcare.ui.ReadyHomeScreen
import dev.defdo.selfcare.ui.SignedOutScreen
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var appConfig: AppConfig
    private lateinit var authClient: DefdoAuthMobileClient
    private lateinit var viewModel: AppViewModel
    private var currentState: String = ""

    private val themeMode: ThemeMode
        get() = if (resources.configuration.isNightMode()) ThemeMode.DARK else ThemeMode.LIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appConfig = AppConfig.fromBuildConfig()
        authClient = buildAuthClient(appConfig)

        val sessionCoordinator = AuthSessionCoordinator(AuthClientSessionProvider(authClient))
        val httpClient = UrlConnectionHttpClient()
        val bootstrapClient = BootstrapClient(httpClient, appConfig.bootstrapEndpoint)
        val themeCache = FileThemeCache(File(cacheDir, "theme"))
        val themeClient = ThemeClient(httpClient, appConfig.themeEndpoint)
        val themeRepository = ThemeRepository(themeClient, themeCache)
        val coordinator = AppStartupCoordinator(
            session = sessionCoordinator,
            bootstrapClient = bootstrapClient,
            themeRepository = themeRepository,
            devDiagnostics = BuildConfig.DEV_DIAGNOSTICS
        )

        viewModel = AppViewModel(coordinator, themeMode)

        setContent {
            val appState by viewModel.appState.collectAsState()
            val theme by viewModel.theme.collectAsState()

            when (val state = appState) {
                is AppState.Launch -> LaunchScreen(theme.tokens)
                is AppState.SignedOut -> SignedOutScreen(theme.tokens, onLogin = ::startLogin)
                is AppState.BootstrapLoading -> BootstrapLoadingScreen(theme.tokens)
                is AppState.NeedsLineLinking -> NeedsLineLinkingScreen(theme.tokens)
                is AppState.ReadyHome -> ReadyHomeScreen(theme.tokens, state.context.tenantCode)
                is AppState.Error -> ErrorScreen(
                    theme.tokens,
                    state.message,
                    onRetry = if (state.retryable) viewModel::retry else null
                )
            }
        }

        viewModel.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallback(intent)
    }

    private fun startLogin() {
        if (!appConfig.isConfigured) return
        currentState = UUID.randomUUID().toString()
        val config = AuthConfig(
            clientId = appConfig.clientId,
            discoveryUrl = appConfig.discoveryUrl,
            redirectUri = appConfig.redirectUri,
            scopes = appConfig.scopes
        )
        val request = LoginRequest(
            config,
            currentState,
            UUID.randomUUID().toString(),
            PKCE.generateVerifier()
        )
        authClient.startLogin(request)
    }

    private fun handleCallback(intent: Intent) {
        val result = AppLinkCallbackHandler.handle(
            intent,
            authClient,
            currentState,
            appConfig.redirectUri
        )
        if (result is LoginResult.Authenticated) {
            viewModel.onLoggedIn()
        }
    }

    private fun buildAuthClient(config: AppConfig): DefdoAuthMobileClient {
        val authConfig = AuthConfig(
            clientId = config.clientId,
            discoveryUrl = config.discoveryUrl,
            redirectUri = config.redirectUri,
            scopes = config.scopes
        )
        val discovery = OAuthDiscoveryDocument(
            issuer = config.issuer,
            authorizationEndpoint = "${config.issuer}/oauth/authorize",
            tokenEndpoint = "${config.issuer}/oauth/token",
            revocationEndpoint = "${config.issuer}/oauth/revoke"
        )
        val storage = AndroidKeystoreSecureStorageAdapter(this)
        val store = SecureTokenStore(storage)
        return DefdoAuthMobileClientImpl(
            authConfig,
            discovery,
            ChromeCustomTabsBrowserAdapter(this),
            store,
            UrlConnectionTokenHttpTransport()
        )
    }
}

private fun android.content.res.Configuration.isNightMode(): Boolean =
    (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
