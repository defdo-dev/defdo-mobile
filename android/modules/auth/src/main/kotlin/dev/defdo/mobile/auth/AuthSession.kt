package dev.defdo.mobile.auth

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresInSeconds: Long,
    val scope: String,
    val tokenType: String = "Bearer"
)
