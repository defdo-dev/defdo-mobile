package dev.defdo.selfcare.startup

import dev.defdo.selfcare.bootstrap.AccessContext
import dev.defdo.selfcare.theme.AppliedTheme

/** High-level app shell states the UI navigates between. */
sealed class AppState {
    object Launch : AppState()
    object SignedOut : AppState()
    object BootstrapLoading : AppState()
    data class NeedsLineLinking(val context: AccessContext) : AppState()
    data class ReadyHome(val context: AccessContext) : AppState()

    /** Retryable error keeps the user signed in (e.g. network failure). */
    data class Error(val message: String, val retryable: Boolean) : AppState()
}

/** Theme + diagnostic state, applied independently of [AppState]. */
data class ThemeState(
    val theme: AppliedTheme,
    val diagnostic: String? = null
)
