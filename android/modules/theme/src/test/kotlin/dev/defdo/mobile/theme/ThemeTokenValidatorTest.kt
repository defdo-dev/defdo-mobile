package dev.defdo.mobile.theme

fun runThemeTokenValidatorTest() {
    val tokens = ThemeTokens(
        schemaVersion = 1,
        themeVersion = "test",
        mode = ThemeMode.LIGHT,
        tokens = ThemeTokenValidator.requiredTokens.associateWith { "#112233" }
    )
    check(ThemeTokenValidator.validate(tokens) == null)
    check(ThemeTokenValidator.validate(tokens.copy(tokens = mapOf("color.background.primary" to "#FFFFFF"))) is ThemeError.MissingTokens)
}
