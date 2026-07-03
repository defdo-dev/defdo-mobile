package dev.defdo.selfcare.startup

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.selfcare.bootstrap.BootstrapClient
import dev.defdo.selfcare.bootstrap.BootstrapResult
import dev.defdo.selfcare.session.AuthSessionCoordinator
import dev.defdo.selfcare.theme.AppliedTheme
import dev.defdo.selfcare.theme.ThemeRefresh
import dev.defdo.selfcare.theme.ThemeRepository

/**
 * Orchestrates the app shell startup flow. Pure logic, no Android imports, so it
 * is fully covered by JVM unit tests. The Activity/ViewModel calls these on a
 * background dispatcher and renders the returned states.
 *
 * Flow:
 *  1. Load embedded fallback theme immediately (localTheme()).
 *  2. Overlay last-known-good cache if present (localTheme()).
 *  3. Check token session from secure storage.
 *  4. No token -> SignedOut.
 *  5. After login / with token -> POST /mobile/bootstrap.
 *  6. needs_line_linking -> NeedsLineLinking; ready -> ReadyHome.
 *  7. In parallel, when token exists -> GET /mobile/theme (If-None-Match).
 *
 * 403 from bootstrap is terminal (no infinite retry): it produces a
 * non-retryable Error. Network errors keep the user signed in and are
 * retryable.
 */
class AppStartupCoordinator(
    private val session: AuthSessionCoordinator,
    private val bootstrapClient: BootstrapClient,
    private val themeRepository: ThemeRepository,
    private val devDiagnostics: Boolean = false
) {
    /** Steps 1-2: theme available before any network. */
    fun initialTheme(mode: ThemeMode): AppliedTheme = themeRepository.localTheme(mode)

    /** Steps 3-6: resolve the authoritative app state. */
    fun resolveAppState(): AppState {
        val token = session.accessToken()
        if (token.isNullOrBlank()) {
            return AppState.SignedOut
        }
        return when (val result = bootstrapClient.bootstrap(token)) {
            is BootstrapResult.Ready -> AppState.ReadyHome(result.context)
            is BootstrapResult.NeedsLineLinking -> AppState.NeedsLineLinking(result.context)
            is BootstrapResult.Unauthorized -> {
                session.invalidate()
                AppState.SignedOut
            }
            is BootstrapResult.Forbidden ->
                AppState.Error(result.message, retryable = false)
            is BootstrapResult.NetworkError ->
                AppState.Error("Can't reach the server. Check your connection.", retryable = true)
            is BootstrapResult.MalformedResponse ->
                AppState.Error("Something went wrong. Please try again.", retryable = false)
        }
    }

    /**
     * Step 7: refresh theme from network. Returns the theme/diagnostic to apply,
     * or null when nothing changes. A returned [ThemeState] with diagnostic set
     * is only meaningful in dev builds.
     *
     * @param onSessionInvalid invoked on theme 401 so the caller can sign out.
     */
    fun refreshTheme(mode: ThemeMode, onSessionInvalid: () -> Unit): ThemeState? {
        val token = session.accessToken() ?: return null
        return when (val refresh = themeRepository.refresh(token, mode)) {
            is ThemeRefresh.Applied -> ThemeState(refresh.theme)
            is ThemeRefresh.UsedCache -> ThemeState(themeRepository.localTheme(mode))
            is ThemeRefresh.KeptFallback -> ThemeState(themeRepository.localTheme(mode))
            is ThemeRefresh.SessionInvalid -> {
                session.invalidate()
                onSessionInvalid()
                null
            }
            is ThemeRefresh.Diagnostic ->
                if (devDiagnostics) {
                    ThemeState(themeRepository.localTheme(mode), diagnostic = refresh.message)
                } else {
                    null
                }
        }
    }
}
