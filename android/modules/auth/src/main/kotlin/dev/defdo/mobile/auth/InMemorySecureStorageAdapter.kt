package dev.defdo.mobile.auth

class InMemorySecureStorageAdapter : SecureStorageAdapter {
    private val store = mutableMapOf<String, ByteArray>()

    override fun get(key: String): ByteArray? = store[key]?.copyOf()

    override fun put(key: String, value: ByteArray) {
        store[key] = value.copyOf()
    }

    override fun delete(key: String) {
        store.remove(key)
    }

    fun clear() {
        store.clear()
    }
}
