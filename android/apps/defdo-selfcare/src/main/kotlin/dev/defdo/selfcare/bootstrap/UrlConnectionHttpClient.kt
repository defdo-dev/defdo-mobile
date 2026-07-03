package dev.defdo.selfcare.bootstrap

import java.net.HttpURLConnection
import java.net.URL

/**
 * Android/JVM HTTP transport using HttpURLConnection. Never logs the
 * Authorization header or response bodies. Mirrors the auth module's
 * UrlConnectionTokenHttpTransport style.
 */
class UrlConnectionHttpClient : HttpClient {
    override fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..399) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val responseHeaders = connection.headerFields
                .filterKeys { it != null }
                .mapValues { it.value.firstOrNull() ?: "" }
            return HttpResponse(status, responseHeaders, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
