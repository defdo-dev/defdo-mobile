package dev.defdo.mobile.theme

class ThemeRepository(
    private val transport: ThemeTransport,
    private val cache: ThemeCache,
    private val fallback: ThemeTokens
) {
    fun current(mode: ThemeMode): Result<ThemeTokens> {
        val cached = cache.read(mode)
        if (cached != null) return Result.success(cached)
        val remote = transport.fetch(mode).getOrNull()
        if (remote != null && ThemeTokenValidator.validate(remote) == null) {
            cache.write(remote)
            return Result.success(remote)
        }
        return Result.success(fallback)
    }
}
