package dev.defdo.mobile.auth

interface TokenStore {
    fun read(): AuthSession?
    fun write(session: AuthSession)
    fun clear()
}
