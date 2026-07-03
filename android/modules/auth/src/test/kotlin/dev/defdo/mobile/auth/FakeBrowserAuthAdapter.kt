package dev.defdo.mobile.auth

class FakeBrowserAuthAdapter : BrowserAuthAdapter {
    val openedUrls = mutableListOf<String>()

    override fun openAuthorizationUrl(url: String) {
        openedUrls.add(url)
    }
}
