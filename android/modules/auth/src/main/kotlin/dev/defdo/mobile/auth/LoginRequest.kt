package dev.defdo.mobile.auth

data class LoginRequest(
    val config: AuthConfig,
    val state: String,
    val nonce: String?,
    val codeVerifier: String
)
