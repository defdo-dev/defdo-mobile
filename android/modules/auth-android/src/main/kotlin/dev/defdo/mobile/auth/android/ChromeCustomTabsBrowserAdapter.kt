package dev.defdo.mobile.auth.android

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dev.defdo.mobile.auth.BrowserAuthAdapter

class ChromeCustomTabsBrowserAdapter(private val context: Context) : BrowserAuthAdapter {
    override fun openAuthorizationUrl(url: String) {
        val uri = Uri.parse(url)
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(context, uri)
    }

    companion object {
        const val CALLBACK_PATH = "/mobile/oauth/callback"
    }
}
