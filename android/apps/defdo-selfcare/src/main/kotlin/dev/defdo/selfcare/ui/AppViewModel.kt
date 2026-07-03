package dev.defdo.selfcare.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.selfcare.startup.AppState
import dev.defdo.selfcare.startup.AppStartupCoordinator
import dev.defdo.selfcare.theme.AppliedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the app shell. Bootstrap and theme run on IO; theme refresh runs in
 * parallel with bootstrap and never blocks navigation.
 */
class AppViewModel(
    private val coordinator: AppStartupCoordinator,
    private val mode: ThemeMode
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Launch)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _theme = MutableStateFlow(coordinator.initialTheme(mode))
    val theme: StateFlow<AppliedTheme> = _theme.asStateFlow()

    private val _diagnostic = MutableStateFlow<String?>(null)
    val diagnostic: StateFlow<String?> = _diagnostic.asStateFlow()

    /** Steps 3-7: resolve state + refresh theme in parallel. */
    fun start() {
        // Theme refresh (step 7) — independent, non-blocking.
        viewModelScope.launch {
            val themeState = withContext(Dispatchers.IO) {
                coordinator.refreshTheme(mode, onSessionInvalid = {
                    _appState.value = AppState.SignedOut
                })
            }
            themeState?.let {
                _theme.value = it.theme
                _diagnostic.value = it.diagnostic
            }
        }

        // App state (steps 3-6).
        viewModelScope.launch {
            _appState.value = AppState.BootstrapLoading
            val state = withContext(Dispatchers.IO) { coordinator.resolveAppState() }
            _appState.value = state
        }
    }

    fun onLoggedIn() = start()

    fun retry() = start()
}
