package dev.defdo.selfcare.theme

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.mobile.theme.ThemeTokens
import dev.defdo.mobile.theme.ThemeTokenValidator
import dev.defdo.selfcare.bootstrap.Json

/**
 * Last-known-good cache envelope. Persists everything required to validate and
 * re-apply a previously fetched runtime theme, plus the ETag for conditional
 * requests.
 */
data class CachedTheme(
    val body: String,
    val etag: String?,
    val fetchedAt: Long,
    val schemaVersion: Int,
    val themeVersion: String,
    val tokens: ThemeTokens
)

/** App-local theme cache abstraction (persisted in the Android layer). */
interface ThemeCache {
    fun read(mode: ThemeMode): CachedTheme?
    fun write(cached: CachedTheme)
    fun clear()
}

/** In-memory cache for unit tests. */
class InMemoryThemeCache : ThemeCache {
    private val store = mutableMapOf<ThemeMode, CachedTheme>()
    override fun read(mode: ThemeMode): CachedTheme? = store[mode]
    override fun write(cached: CachedTheme) { store[cached.tokens.mode] = cached }
    override fun clear() = store.clear()
}

/**
 * Parses + validates a runtime theme body. Returns null when the body is
 * malformed or fails validation, so callers can discard it and fall back.
 */
object ThemeCodec {
    fun parse(body: String): ThemeTokens? {
        val json = try {
            Json.parseObject(body)
        } catch (e: Exception) {
            return null
        }
        val schemaVersion = (json["schema_version"] as? Long)?.toInt() ?: return null
        // Validate schema_version == 1 before trusting anything else.
        if (schemaVersion != 1) return null
        val themeVersion = json["theme_version"] as? String ?: return null
        val modeStr = json["mode"] as? String ?: return null
        val mode = when (modeStr.lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> return null
        }
        val tokens = Json.stringMap(json["tokens"]) ?: return null
        val parsed = ThemeTokens(
            schemaVersion = schemaVersion,
            themeVersion = themeVersion,
            mode = mode,
            tokens = tokens
        )
        // Reuse the shared validator: required tokens present + colors parseable.
        if (ThemeTokenValidator.validate(parsed) != null) return null
        return parsed
    }
}
