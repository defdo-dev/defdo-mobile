package dev.defdo.mobile.theme

class ComposeThemeAdapter : ThemeAdapter {
    override fun map(tokens: ThemeTokens): Map<String, String> = tokens.tokens
}
