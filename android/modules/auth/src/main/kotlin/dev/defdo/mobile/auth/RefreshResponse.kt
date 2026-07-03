package dev.defdo.mobile.auth

data class RefreshResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val idToken: String?,
    val tokenType: String?,
    val expiresIn: Long?,
    val scope: String?,
    val error: String?
) {
    val isSuccess: Boolean = !accessToken.isNullOrBlank() && error.isNullOrBlank()
    val hasRotatedRefreshToken: Boolean = !refreshToken.isNullOrBlank()
    val requiresLogin: Boolean = error == "invalid_grant"
    val isAcceptableTokenType: Boolean
        get() = tokenType == null || tokenType.equals("Bearer", ignoreCase = true)

    companion object {
        fun parse(map: Map<String, Any?>): RefreshResponse {
            val err = map["error"] as? String
            return RefreshResponse(
                accessToken = map["access_token"] as? String,
                refreshToken = map["refresh_token"] as? String,
                idToken = map["id_token"] as? String,
                tokenType = map["token_type"] as? String,
                expiresIn = (map["expires_in"] as? Number)?.toLong(),
                scope = map["scope"] as? String,
                error = err
            )
        }
    }
}
