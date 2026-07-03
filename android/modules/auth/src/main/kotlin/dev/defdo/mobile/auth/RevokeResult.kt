package dev.defdo.mobile.auth

sealed class RevokeResult {
    data class Success(val clearLocalSession: Boolean = true) : RevokeResult()
    data class Failure(val error: AuthError, val clearLocalSession: Boolean = true) : RevokeResult()
}
