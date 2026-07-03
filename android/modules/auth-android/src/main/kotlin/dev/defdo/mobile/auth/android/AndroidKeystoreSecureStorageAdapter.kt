package dev.defdo.mobile.auth.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.defdo.mobile.auth.SecureStorageAdapter

class AndroidKeystoreSecureStorageAdapter(context: Context) : SecureStorageAdapter {
    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun get(key: String): ByteArray? {
        val encoded = prefs.getString(key, null) ?: return null
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    override fun put(key: String, value: ByteArray) {
        val encoded = android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)
        prefs.edit().putString(key, encoded).apply()
    }

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    companion object {
        const val KEY_ALIAS = "defdo_auth_master_key"
        const val PREFS_FILE = "defdo_auth_secure_storage"
    }
}
