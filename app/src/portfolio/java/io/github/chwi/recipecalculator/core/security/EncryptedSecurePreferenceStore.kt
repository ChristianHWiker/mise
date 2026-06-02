package io.github.chwi.recipecalculator.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 lands here — `EncryptedSharedPreferences` with a `MasterKey` from the Android
 * Keystore. Currently a no-op stub so the portfolio flavor compiles without the
 * `androidx.security:security-crypto` dep on the classpath; Phase 4 adds the dep and swaps
 * the bodies.
 */
@Singleton
class EncryptedSecurePreferenceStore @Inject constructor() : SecurePreferenceStore {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean = default
    override suspend fun putBoolean(key: String, value: Boolean) = Unit
    override suspend fun getString(key: String, default: String?): String? = default
    override suspend fun putString(key: String, value: String?) = Unit
}
