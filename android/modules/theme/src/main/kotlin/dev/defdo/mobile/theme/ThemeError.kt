package dev.defdo.mobile.theme

sealed class ThemeError {
    data class MissingTokens(val tokens: List<String>) : ThemeError()
    data class InvalidColor(val tokens: List<String>) : ThemeError()
    object TransportFailed : ThemeError()
}
