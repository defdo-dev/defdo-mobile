package dev.defdo.mobile.auth

interface TokenHttpTransport {
    fun exchangeCode(tokenEndpoint: String, params: Map<String, String>): Result<TokenResponse>
    fun refreshToken(tokenEndpoint: String, params: Map<String, String>): Result<RefreshResponse>
    fun revokeToken(revocationEndpoint: String, params: Map<String, String>): Result<Unit>
}
