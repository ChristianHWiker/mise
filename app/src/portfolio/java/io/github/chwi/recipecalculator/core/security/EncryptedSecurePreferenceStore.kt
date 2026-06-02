package io.github.chwi.recipecalculator.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portfolio flavor: real [SecurePreferenceStore] backed by `EncryptedSharedPreferences` with a
 * `MasterKey` alias held in the Android Keystore (hardware-backed where available —
 * TEE/StrongBox on Pixels). Reads/writes are wrapped in [Dispatchers.IO] because the underlying
 * `SharedPreferences` API is synchronous and would otherwise stall whichever coroutine called us.
 *
 * The on-disk file is `shared_prefs/secure_prefs.xml`; the backup rules in
 * `res/xml/data_extraction_rules.xml` exclude it from cloud backup because the Keystore key
 * is device-bound and the ciphertext would be unreadable on a different device.
 *
 * Uses the 1.0.0 stable `MasterKeys` helper rather than the 1.1.0-alpha `MasterKey.Builder` —
 * the alpha line has stayed alpha for years and the AndroidX security library is in
 * maintenance mode. The 1.0.0 API has reversed parameter order (fileName before context).
 */
@Singleton
class EncryptedSecurePreferenceStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecurePreferenceStore {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        withContext(Dispatchers.IO) { prefs.getBoolean(key, default) }

    override suspend fun putBoolean(key: String, value: Boolean) {
        withContext(Dispatchers.IO) { prefs.edit().putBoolean(key, value).apply() }
    }

    override suspend fun getString(key: String, default: String?): String? =
        withContext(Dispatchers.IO) { prefs.getString(key, default) }

    override suspend fun putString(key: String, value: String?) {
        withContext(Dispatchers.IO) {
            prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
        }
    }

    private companion object {
        // Must match the exclude path in res/xml/data_extraction_rules.xml.
        const val FILE_NAME = "secure_prefs"
    }
}
