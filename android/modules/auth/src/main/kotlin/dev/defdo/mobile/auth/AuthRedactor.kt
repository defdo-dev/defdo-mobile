package dev.defdo.mobile.auth

object AuthRedactor {
    private val sensitiveKeys = listOf("access_token", "refresh_token", "id_token", "code", "code_verifier", "claims")
    private const val replacement = "[REDACTED]"

    fun redact(input: String): String {
        var output = input

        output = redactClaimsStructural(output)

        for (key in sensitiveKeys) {
            output = output
                .replace(Regex("($key=)[^&\\s]+"), "$1$replacement")
                .replace(Regex("(\"$key\"\\s*:\\s*)\"((?:\\\\\"|[^\"])*)\""), "\"$key\":\"$replacement\"")
                .replace(Regex("(\"$key\"\\s*:\\s*\")[^\"]*\""), "$1$replacement\"")
        }

        output = output.replace(Regex("Authorization\\s*:\\s*[^\\r\\n]+", RegexOption.IGNORE_CASE)) {
            "Authorization: $replacement"
        }
        output = output.replace(Regex("Set-Cookie\\s*:\\s*[^\\r\\n]+", RegexOption.IGNORE_CASE)) {
            "Set-Cookie: $replacement"
        }

        return output
    }

    private fun redactClaimsStructural(input: String): String {
        var output = input
        val claimsPatterns = listOf(
            Regex("\"claims\"\\s*:\\s*\\{[^}]*\\}"),
            Regex("\"claims\"\\s*:\\s*\\[[^\\]]*\\]")
        )
        for (pattern in claimsPatterns) {
            output = pattern.replace(output) {
                val prefix = it.value.substringBefore('{').substringBefore('[')
                "$prefix$replacement\""
            }
        }
        return output
    }
}
