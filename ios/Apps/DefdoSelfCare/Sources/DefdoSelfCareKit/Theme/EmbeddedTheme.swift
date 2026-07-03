import DefdoThemeMobile

/// Embedded fallback theme — always available, no network, never expires.
/// Values mirror shared-contracts/theme/fallback_theme.*.fixture.json.
public enum EmbeddedTheme {
    public static func forMode(_ mode: ThemeMode) -> ThemeTokens {
        mode == .dark ? dark : light
    }

    public static let light = ThemeTokens(
        schemaVersion: 1,
        themeVersion: "fallback-light-1",
        mode: .light,
        tokens: [
            "color.background.primary": "#FFFFFF",
            "color.background.surface": "#F4F6F8",
            "color.text.primary": "#101418",
            "color.text.muted": "#4B5563",
            "color.action.primary.background": "#145DCC",
            "color.action.primary.text": "#FFFFFF",
            "color.status.success.background": "#DDF8E8",
            "color.status.warning.text": "#5C4100",
            "color.input.border.default": "#AEB7C2",
            "color.input.border.focused": "#145DCC"
        ]
    )

    public static let dark = ThemeTokens(
        schemaVersion: 1,
        themeVersion: "fallback-dark-1",
        mode: .dark,
        tokens: [
            "color.background.primary": "#0B0D10",
            "color.background.surface": "#151922",
            "color.text.primary": "#F7F8FA",
            "color.text.muted": "#AAB1BD",
            "color.action.primary.background": "#7EA2FF",
            "color.action.primary.text": "#05070A",
            "color.status.success.background": "#103B2A",
            "color.status.warning.text": "#FFE08A",
            "color.input.border.default": "#3A4050",
            "color.input.border.focused": "#7EA2FF"
        ]
    )
}
