package dev.defdo.selfcare.bootstrap

/**
 * Authoritative app-state derived from POST /mobile/bootstrap.
 *
 * AccessContext is the backend's answer for "who/what is this token allowed to
 * act as" — brand/app/tenant come FROM here, never sent BY the app.
 */
data class AccessContext(
    val brandKey: String?,
    val appKey: String?,
    val tenantCode: String?,
    val themeEndpoint: String?
)

sealed class BootstrapResult {
    /** 200 with status=ready. */
    data class Ready(val context: AccessContext) : BootstrapResult()

    /** 200 with status=needs_line_linking. */
    data class NeedsLineLinking(val context: AccessContext) : BootstrapResult()

    /** 401 — token invalid/missing/inactive. Caller clears session. */
    object Unauthorized : BootstrapResult()

    /** 403 — valid token but app/actor not allowed. Show safe error, do not retry. */
    data class Forbidden(val message: String) : BootstrapResult()

    /** Network/transport failure — keep signed-in state, offer retry. */
    data class NetworkError(val message: String) : BootstrapResult()

    /** Malformed/unexpected response — safe error. */
    data class MalformedResponse(val message: String) : BootstrapResult()
}
