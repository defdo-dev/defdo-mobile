package dev.defdo.selfcare.theme

import dev.defdo.mobile.theme.ThemeMode
import dev.defdo.mobile.theme.ThemeTokens

/**
 * Embedded fallback theme — always available, no network, never expires.
 * Values mirror shared-contracts/theme/fallback_theme.*.fixture.json.
 *
 * The Android layer loads the same JSON from assets; this is the in-code
 * backstop so the app shell can render even if assets are unreadable.
 */
object EmbeddedTheme {
    fun forMode(mode: ThemeMode): ThemeTokens =
        if (mode == ThemeMode.DARK) dark else light

    val light = ThemeTokens(
        schemaVersion = 1,
        themeVersion = "fallback-light-1",
        mode = ThemeMode.LIGHT,
        tokens = mapOf(
            "color.background.primary" to "#FFFFFF",
            "color.background.surface" to "#F4F6F8",
            "color.text.primary" to "#101418",
            "color.text.muted" to "#4B5563",
            "color.action.primary.background" to "#145DCC",
            "color.action.primary.text" to "#FFFFFF",
            "color.status.success.background" to "#DDF8E8",
            "color.status.warning.text" to "#5C4100",
            "color.input.border.default" to "#AEB7C2",
            "color.input.border.focused" to "#145DCC"
        )
    )

    val dark = ThemeTokens(
        schemaVersion = 1,
        themeVersion = "fallback-dark-1",
        mode = ThemeMode.DARK,
        tokens = mapOf(
            "color.background.primary" to "#0B0D10",
            "color.background.surface" to "#151922",
            "color.text.primary" to "#F7F8FA",
            "color.text.muted" to "#AAB1BD",
            "color.action.primary.background" to "#7EA2FF",
            "color.action.primary.text" to "#05070A",
            "color.status.success.background" to "#103B2A",
            "color.status.warning.text" to "#FFE08A",
            "color.input.border.default" to "#3A4050",
            "color.input.border.focused" to "#7EA2FF"
        )
    )
}
