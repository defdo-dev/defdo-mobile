package dev.defdo.mobile.auth

interface SecureStorageAdapter {
    fun get(key: String): ByteArray?
    fun put(key: String, value: ByteArray)
    fun delete(key: String)
}
