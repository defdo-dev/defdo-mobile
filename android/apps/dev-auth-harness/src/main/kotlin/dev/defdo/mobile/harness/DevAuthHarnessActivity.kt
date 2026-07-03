package dev.defdo.mobile.harness

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.defdo.mobile.auth.*
import dev.defdo.mobile.auth.android.AndroidKeystoreSecureStorageAdapter
import dev.defdo.mobile.auth.android.AppLinkCallbackHandler
import dev.defdo.mobile.auth.android.ChromeCustomTabsBrowserAdapter

class DevAuthHarnessActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var loginButton: Button
    private lateinit var refreshButton: Button
    private lateinit var logoutButton: Button
    private lateinit var clearButton: Button

    private var client: DefdoAuthMobileClient? = null
    private var currentState: String = ""
    private var config: AuthConfig? = null
    private var discovery: OAuthDiscoveryDocument? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev_auth_harness)

        statusText = findViewById(R.id.statusText)
        loginButton = findViewById(R.id.loginButton)
        refreshButton = findViewById(R.id.refreshButton)
        logoutButton = findViewById(R.id.logoutButton)
        clearButton = findViewById(R.id.clearButton)

        val issuer = System.getenv("DEFDO_DEV_ISSUER")
        if (issuer.isNullOrBlank()) {
            statusText.text = getString(R.string.not_configured)
            return
        }
        val discoveryUrl = System.getenv("DEFDO_DEV_DISCOVERY_URL")
            ?: "$issuer/.well-known/openid-configuration"
        val clientId = System.getenv("DEFDO_DEV_CLIENT_ID")
            ?: "dev-client"
        val redirectUri = System.getenv("DEFDO_DEV_REDIRECT_URI")
            ?: "https://app.defdo.example/oauth/callback"
        val scopes = (System.getenv("DEFDO_DEV_SCOPES")
            ?: "openid profile offline_access").split(" ")

        try {
            config = AuthConfig(
                clientId = clientId,
                discoveryUrl = discoveryUrl,
                redirectUri = redirectUri,
                scopes = scopes
            )
            discovery = OAuthDiscoveryDocument(
                issuer = issuer,
                authorizationEndpoint = "$issuer/oauth/authorize",
                tokenEndpoint = "$issuer/oauth/token",
                revocationEndpoint = "$issuer/oauth/revoke"
            )
            discovery!!.validate(issuer)?.let { error ->
                setStatus("discovery invalid: ${AuthRedactor.redact(error.message ?: "")}")
                return
            }

            val transport = UrlConnectionTokenHttpTransport()
            val storage = AndroidKeystoreSecureStorageAdapter(this)
            val store = SecureTokenStore(storage)
            val browser = ChromeCustomTabsBrowserAdapter(this)
            client = DefdoAuthMobileClientImpl(config!!, discovery!!, browser, store, transport)

            setStatus("configured: $issuer")
            enableButtons(true)
        } catch (e: Exception) {
            setStatus("init error: ${AuthRedactor.redact(e.message ?: "")}")
        }

        loginButton.setOnClickListener { startLogin() }
        refreshButton.setOnClickListener { doRefresh() }
        logoutButton.setOnClickListener { doLogout() }
        clearButton.setOnClickListener { doClear() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallbackIntent(intent)
    }

    private fun handleCallbackIntent(intent: Intent) {
        val c = client ?: return
        val redirectUri = config?.redirectUri ?: return
        val result = AppLinkCallbackHandler.handle(intent, c, currentState, redirectUri)
        when (result) {
            is LoginResult.Authenticated -> setStatus("authenticated")
            is LoginResult.Failed -> setStatus("login failed: ${AuthRedactor.redact(result.error.message ?: "")}")
            else -> setStatus("unexpected result")
        }
    }

    private fun startLogin() {
        val c = client ?: return
        val cfg = config ?: return
        currentState = java.util.UUID.randomUUID().toString()
        val verifier = PKCE.generateVerifier()
        val nonce = java.util.UUID.randomUUID().toString()
        val request = LoginRequest(cfg, currentState, nonce, verifier)
        val result = c.startLogin(request)
        setStatus("login started")
    }

    private fun doRefresh() {
        val c = client ?: return
        val session = c.currentSession() ?: run {
            setStatus("no session")
            return
        }
        Thread {
            val result = c.refresh(session)
            runOnUiThread {
                when (result) {
                    is LoginResult.Authenticated -> setStatus("refresh succeeded")
                    is LoginResult.Failed -> setStatus("refresh failed: ${AuthRedactor.redact(result.error.message ?: "")}")
                    else -> setStatus("unexpected refresh result")
                }
            }
        }.start()
    }

    private fun doLogout() {
        val c = client ?: return
        val session = c.currentSession() ?: run {
            setStatus("no session")
            return
        }
        Thread {
            c.revoke(session)
            runOnUiThread { setStatus("logged out") }
        }.start()
    }

    private fun doClear() {
        val c = client ?: return
        val session = c.currentSession() ?: run {
            setStatus("no session")
            return
        }
        c.revoke(session)
        setStatus("session cleared")
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    private fun enableButtons(enabled: Boolean) {
        loginButton.isEnabled = enabled
        refreshButton.isEnabled = enabled
        logoutButton.isEnabled = enabled
        clearButton.isEnabled = enabled
    }
}
