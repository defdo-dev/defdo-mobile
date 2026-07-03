package dev.defdo.mobile.theme

fun runThemeContractTest() {
    check(ThemeTokenValidator.requiredTokens.contains("color.action.primary.background"))
    check(ThemeTokenValidator.requiredTokens.contains("color.input.border.focused"))
}
