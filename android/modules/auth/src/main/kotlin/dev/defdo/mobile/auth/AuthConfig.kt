package dev.defdo.mobile.auth

data class AuthConfig(
    val clientId: String,
    val discoveryUrl: String,
    val redirectUri: String,
    val scopes: List<String>,
    val useNonce: Boolean = true
)
