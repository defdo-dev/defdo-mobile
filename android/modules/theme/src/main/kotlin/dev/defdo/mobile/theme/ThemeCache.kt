package dev.defdo.mobile.theme

interface ThemeCache {
    fun read(mode: ThemeMode): ThemeTokens?
    fun write(tokens: ThemeTokens)
}
