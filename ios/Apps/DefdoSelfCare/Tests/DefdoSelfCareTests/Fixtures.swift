enum Fixtures {
    static let readyBootstrap = """
    {
      "status": "ready",
      "tenant": { "code": "defdo-telecom", "name": "Defdo Telecom" },
      "brand": { "key": "defdo-telecom" },
      "app": { "key": "selfcare" },
      "theme": { "endpoint": "/mobile/theme" }
    }
    """

    static let needsLineLinkingBootstrap = """
    {
      "status": "needs_line_linking",
      "tenant": { "code": "defdo-telecom" },
      "theme": { "endpoint": "/mobile/theme" }
    }
    """

    static let malformedBootstrap = "{ \"unexpected\": true }"

    static let themeSuccess = """
    {
      "schema_version": 1,
      "theme_version": "dev-theme-1",
      "mode": "light",
      "tokens": {
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
      }
    }
    """

    static let themeWrongSchema = """
    {
      "schema_version": 2,
      "theme_version": "bad-1",
      "mode": "light",
      "tokens": { "color.background.primary": "#FFFFFF" }
    }
    """

    static let themeInvalidColor = """
    {
      "schema_version": 1,
      "theme_version": "bad-color-1",
      "mode": "light",
      "tokens": {
        "color.background.primary": "not-a-color",
        "color.background.surface": "#F4F6F8",
        "color.text.primary": "#101418",
        "color.text.muted": "#4B5563",
        "color.action.primary.background": "#145DCC",
        "color.action.primary.text": "#FFFFFF",
        "color.status.success.background": "#DDF8E8",
        "color.status.warning.text": "#5C4100",
        "color.input.border.default": "#AEB7C2",
        "color.input.border.focused": "#145DCC"
      }
    }
    """
}
