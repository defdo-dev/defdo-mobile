package dev.defdo.selfcare.bootstrap

/**
 * Minimal HTTP abstraction so coordinators stay pure-JVM testable with fakes.
 * Real platform transport lives in the Android layer (UrlConnectionHttpClient).
 */
interface HttpClient {
    fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse
}

data class HttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val body: String
) {
    /** Case-insensitive header lookup. */
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}
