package dev.defdo.selfcare.bootstrap

/**
 * Client for POST /mobile/bootstrap — the authoritative app-state call.
 *
 * Sends ONLY the bearer access token. It deliberately does NOT send
 * brand_code, brand_key, app_key, theme_code, or tenant as authority. The
 * backend derives app/brand/tenant context from the OAuth client behind the
 * token and returns it in the AccessContext.
 */
class BootstrapClient(
    private val http: HttpClient,
    private val endpoint: String
) {
    fun bootstrap(accessToken: String): BootstrapResult {
        val response = try {
            http.request(
                method = "POST",
                url = endpoint,
                headers = mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Accept" to "application/json",
                    "X-Defdo-Platform" to "android"
                ),
                body = "{}"
            )
        } catch (e: Exception) {
            return BootstrapResult.NetworkError(e.message ?: "transport failure")
        }

        return when (response.status) {
            200 -> parseOk(response.body)
            401 -> BootstrapResult.Unauthorized
            403 -> BootstrapResult.Forbidden("Your account is not allowed to use this app.")
            in 500..599 -> BootstrapResult.NetworkError("server error ${response.status}")
            else -> BootstrapResult.MalformedResponse("unexpected status ${response.status}")
        }
    }

    private fun parseOk(body: String): BootstrapResult {
        val json = try {
            Json.parseObject(body)
        } catch (e: Exception) {
            return BootstrapResult.MalformedResponse("invalid bootstrap body")
        }

        val status = json["status"] as? String
            ?: return BootstrapResult.MalformedResponse("missing status")
        val context = parseContext(json)

        return when (status) {
            "ready" -> BootstrapResult.Ready(context)
            "needs_line_linking" -> BootstrapResult.NeedsLineLinking(context)
            else -> BootstrapResult.MalformedResponse("unknown status: $status")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseContext(json: Map<String, Any?>): AccessContext {
        val brand = json["brand"] as? Map<String, Any?>
        val app = json["app"] as? Map<String, Any?>
        val tenant = json["tenant"] as? Map<String, Any?>
        val theme = json["theme"] as? Map<String, Any?>
        return AccessContext(
            brandKey = brand?.get("key") as? String,
            appKey = app?.get("key") as? String,
            tenantCode = tenant?.get("code") as? String,
            // Endpoint hint from the backend (e.g. "/mobile/theme").
            themeEndpoint = (theme?.get("endpoint") as? String)
        )
    }
}
