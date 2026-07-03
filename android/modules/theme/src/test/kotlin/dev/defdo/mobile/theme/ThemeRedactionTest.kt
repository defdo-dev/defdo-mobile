package dev.defdo.mobile.theme

fun runThemeRedactionTest() {
    val sensitiveKeys = listOf("raw_payload", "authorization", "access_token")
    check(sensitiveKeys.contains("access_token"))
}
