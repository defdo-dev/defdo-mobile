package dev.defdo.mobile.auth

interface BrowserAuthAdapter {
    fun openAuthorizationUrl(url: String)
}
