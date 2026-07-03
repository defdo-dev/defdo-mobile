package dev.defdo.mobile.theme

object ThemeTokenValidator {
    val requiredTokens = listOf(
        "color.background.primary",
        "color.background.surface",
        "color.text.primary",
        "color.text.muted",
        "color.action.primary.background",
        "color.action.primary.text",
        "color.status.success.background",
        "color.status.warning.text",
        "color.input.border.default",
        "color.input.border.focused"
    )

    private val colorPattern = Regex("^#[0-9A-Fa-f]{6}$")

    fun validate(tokens: ThemeTokens): ThemeError? {
        val missing = requiredTokens.filterNot { tokens.tokens.containsKey(it) }
        if (missing.isNotEmpty()) return ThemeError.MissingTokens(missing)
        val invalid = tokens.tokens.filterValues { !colorPattern.matches(it) }.keys.toList()
        if (invalid.isNotEmpty()) return ThemeError.InvalidColor(invalid)
        return null
    }
}
