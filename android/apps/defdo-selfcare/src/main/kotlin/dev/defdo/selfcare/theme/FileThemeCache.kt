package dev.defdo.selfcare.theme

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.selfcare.bootstrap.Json
import java.io.File

/**
 * Disk-backed last-known-good theme cache. One file per mode under the app's
 * cache dir. Persists: theme body, ETag, fetched_at, schema_version,
 * theme_version. On read, the body is re-parsed and re-validated via
 * ThemeCodec; any invalid cache is discarded and deleted so the app falls back
 * to embedded.
 */
class FileThemeCache(private val dir: File) : ThemeCache {

    private fun fileFor(mode: ThemeMode): File =
        File(dir, "theme_${mode.name.lowercase()}.json")

    override fun read(mode: ThemeMode): CachedTheme? {
        val file = fileFor(mode)
        if (!file.exists()) return null
        val envelope = try {
            Json.parseObject(file.readText())
        } catch (e: Exception) {
            file.delete()
            return null
        }
        val body = envelope["body"] as? String ?: run { file.delete(); return null }
        // Re-validate the persisted body before trusting it.
        val tokens = ThemeCodec.parse(body) ?: run { file.delete(); return null }
        if (tokens.mode != mode) { file.delete(); return null }
        return CachedTheme(
            body = body,
            etag = envelope["etag"] as? String,
            fetchedAt = (envelope["fetched_at"] as? Long) ?: 0L,
            schemaVersion = tokens.schemaVersion,
            themeVersion = tokens.themeVersion,
            tokens = tokens
        )
    }

    override fun write(cached: CachedTheme) {
        dir.mkdirs()
        val escapedBody = escape(cached.body)
        val escapedEtag = cached.etag?.let { "\"${escape(it)}\"" } ?: "null"
        val json = buildString {
            append("{")
            append("\"body\":\"").append(escapedBody).append("\",")
            append("\"etag\":").append(escapedEtag).append(",")
            append("\"fetched_at\":").append(cached.fetchedAt).append(",")
            append("\"schema_version\":").append(cached.schemaVersion).append(",")
            append("\"theme_version\":\"").append(escape(cached.themeVersion)).append("\"")
            append("}")
        }
        fileFor(cached.tokens.mode).writeText(json)
    }

    override fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}
