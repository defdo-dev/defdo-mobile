package dev.defdo.mobile.theme

interface ThemeTransport {
    fun fetch(mode: ThemeMode): Result<ThemeTokens>
}
