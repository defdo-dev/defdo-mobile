package dev.defdo.mobile.auth.android

import android.content.Intent
import android.net.Uri
import dev.defdo.mobile.auth.DefdoAuthMobileClient
import dev.defdo.mobile.auth.LoginResult

/**
 * Callback handoff surface for Android App Links and custom scheme redirects.
 *
 * Host apps receive the redirect Intent in their Activity and route it here. The raw URI is
 * never logged; validation and sensitive value redaction are handled downstream in the core
 * auth module.
 */
object AppLinkCallbackHandler {
    fun handle(
        intent: Intent,
        client: DefdoAuthMobileClient,
        expectedState: String,
        expectedRedirectUri: String
    ): LoginResult {
        val uri = intent.data ?: return LoginResult.Failed(
            dev.defdo.mobile.auth.AuthError.InvalidCallback("intent has no data URI")
        )
        return handle(uri.toString(), client, expectedState, expectedRedirectUri)
    }

    fun handle(
        callbackUrl: String,
        client: DefdoAuthMobileClient,
        expectedState: String,
        expectedRedirectUri: String
    ): LoginResult {
        return client.handleCallback(callbackUrl, expectedState, expectedRedirectUri)
    }
}
