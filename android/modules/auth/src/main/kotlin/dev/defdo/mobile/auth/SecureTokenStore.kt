package dev.defdo.mobile.auth

class SecureTokenStore(private val storage: SecureStorageAdapter) : TokenStore {
    companion object {
        private const val KEY = "defdo_auth_session"
        private const val SCHEMA_VERSION = 1
    }

    override fun read(): AuthSession? {
        val bytes = storage.get(KEY) ?: return null
        return try {
            val json = String(bytes, Charsets.UTF_8)
            val map = parseSimpleJson(json)
            val version = (map["schema_version"] as? Number)?.toInt() ?: 0
            if (version != SCHEMA_VERSION) return null
            AuthSession(
                accessToken = map["access_token"] as? String ?: return null,
                refreshToken = map["refresh_token"] as? String,
                idToken = map["id_token"] as? String,
                expiresInSeconds = (map["expires_in"] as? Number)?.toLong() ?: 0L,
                scope = map["scope"] as? String ?: "",
                tokenType = map["token_type"] as? String ?: "Bearer"
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun write(session: AuthSession) {
        val now = System.currentTimeMillis() / 1000
        val json = buildString {
            append("{")
            append("\"schema_version\":$SCHEMA_VERSION")
            append(",\"access_token\":\"${escapeJson(session.accessToken)}\"")
            if (session.refreshToken != null) append(",\"refresh_token\":\"${escapeJson(session.refreshToken)}\"")
            if (session.idToken != null) append(",\"id_token\":\"${escapeJson(session.idToken)}\"")
            append(",\"token_type\":\"${escapeJson(session.tokenType)}\"")
            append(",\"expires_in\":${session.expiresInSeconds}")
            append(",\"captured_at\":$now")
            append(",\"scope\":\"${escapeJson(session.scope)}\"")
            append("}")
        }
        storage.put(KEY, json.toByteArray(Charsets.UTF_8))
    }

    override fun clear() {
        storage.delete(KEY)
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder()
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
