package dev.defdo.mobile.theme

class DefdoThemeClient(
    private val repository: ThemeRepository,
    private val adapter: ThemeAdapter
) {
    fun load(mode: ThemeMode): Result<Any> {
        return repository.current(mode).map { adapter.map(it) }
    }
}
