package dev.defdo.mobile.theme

data class ThemeTokens(
    val schemaVersion: Int,
    val themeVersion: String,
    val mode: ThemeMode,
    val tokens: Map<String, String>
)
