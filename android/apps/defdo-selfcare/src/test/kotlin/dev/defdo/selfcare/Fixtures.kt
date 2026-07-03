package dev.defdo.selfcare

object Fixtures {
    val readyBootstrap = """
        {
          "status": "ready",
          "tenant": { "code": "defdo-telecom", "name": "Defdo Telecom" },
          "brand": { "key": "defdo-telecom" },
          "app": { "key": "selfcare" },
          "theme": { "endpoint": "/mobile/theme" }
        }
    """.trimIndent()

    val needsLineLinkingBootstrap = """
        {
          "status": "needs_line_linking",
          "tenant": { "code": "defdo-telecom" },
          "theme": { "endpoint": "/mobile/theme" }
        }
    """.trimIndent()

    val malformedBootstrap = """{ "unexpected": true }"""

    val themeSuccess = """
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
    """.trimIndent()

    // schema_version 2 -> invalid, must be discarded.
    val themeWrongSchema = """
        {
          "schema_version": 2,
          "theme_version": "bad-1",
          "mode": "light",
          "tokens": { "color.background.primary": "#FFFFFF" }
        }
    """.trimIndent()

    // unparseable color -> invalid.
    val themeInvalidColor = """
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
    """.trimIndent()
}
