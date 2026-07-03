package dev.defdo.selfcare

import dev.defdo.selfcare.bootstrap.HttpClient
import dev.defdo.selfcare.bootstrap.HttpResponse
import dev.defdo.selfcare.session.SessionProvider

/**
 * Records every request so tests can assert the app never sends brand_code,
 * brand_key, app_key, theme_code, or tenant as authority — and never logs
 * tokens.
 */
class FakeHttpClient(
    private val responder: (RecordedRequest) -> HttpResponse
) : HttpClient {
    val requests = mutableListOf<RecordedRequest>()
    var throwOnNext: Boolean = false

    override fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse {
        val recorded = RecordedRequest(method, url, headers, body)
        requests.add(recorded)
        if (throwOnNext) {
            throwOnNext = false
            throw java.io.IOException("simulated network failure")
        }
        return responder(recorded)
    }
}

data class RecordedRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?
)

class FakeSessionProvider(initialToken: String?) : SessionProvider {
    var token: String? = initialToken
    var clearCount: Int = 0

    override fun currentAccessToken(): String? = token
    override fun clear() {
        clearCount++
        token = null
    }
}
