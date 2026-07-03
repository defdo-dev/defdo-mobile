package dev.defdo.mobile.theme

fun runThemeFallbackTest() {
    val fallback = ThemeTokens(1, "fallback", ThemeMode.LIGHT, ThemeTokenValidator.requiredTokens.associateWith { "#112233" })
    val repo = ThemeRepository(
        transport = object : ThemeTransport {
            override fun fetch(mode: ThemeMode): Result<ThemeTokens> = Result.failure(RuntimeException("offline"))
        },
        cache = object : ThemeCache {
            override fun read(mode: ThemeMode): ThemeTokens? = null
            override fun write(tokens: ThemeTokens) = Unit
        },
        fallback = fallback
    )
    check(repo.current(ThemeMode.LIGHT).getOrThrow().themeVersion == "fallback")
}
