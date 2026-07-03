package dev.defdo.mobile.auth

class FakeTokenHttpTransport(
    private val exchangeResponse: TokenResponse? = null,
    private val refreshResponse: RefreshResponse? = null,
    private val revokeResult: Result<Unit> = Result.success(Unit),
    private val exchangeError: AuthError? = null,
    private val refreshError: AuthError? = null
) : TokenHttpTransport {

    val exchangeCalls = mutableListOf<Map<String, String>>()
    val refreshCalls = mutableListOf<Map<String, String>>()
    val revokeCalls = mutableListOf<Map<String, String>>()

    override fun exchangeCode(tokenEndpoint: String, params: Map<String, String>): Result<TokenResponse> {
        exchangeCalls.add(params)
        return if (exchangeError != null) {
            Result.failure(exchangeError)
        } else {
            Result.success(requireNotNull(exchangeResponse))
        }
    }

    override fun refreshToken(tokenEndpoint: String, params: Map<String, String>): Result<RefreshResponse> {
        refreshCalls.add(params)
        return if (refreshError != null) {
            Result.failure(refreshError)
        } else {
            Result.success(requireNotNull(refreshResponse))
        }
    }

    override fun revokeToken(revocationEndpoint: String, params: Map<String, String>): Result<Unit> {
        revokeCalls.add(params)
        return revokeResult
    }
}
