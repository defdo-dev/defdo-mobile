package dev.defdo.mobile.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PKCE {
    private val verifierPattern = Regex("^[A-Za-z0-9._~-]{43,128}$")

    fun generateVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isValidVerifier(verifier: String): Boolean = verifierPattern.matches(verifier)

    fun s256Challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
