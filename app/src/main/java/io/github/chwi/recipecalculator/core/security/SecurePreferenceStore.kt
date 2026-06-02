package io.github.chwi.recipecalculator.core.security

/**
 * Flavor seam for storing values that should live behind Keystore-backed encryption when
 * available. The portfolio flavor binds an `EncryptedSharedPreferences`-backed impl; the play
 * flavor binds a no-op that ignores writes and returns defaults on reads (since no production
 * code in play should touch these keys — they live behind [AppLockController.isAvailable]).
 *
 * Kept narrow — booleans + strings are enough for the Phase 4 surface (the biometric-lock flag,
 * the integrity-warning dismissal flag). Widen only when there's a real call site.
 */
interface SecurePreferenceStore {
    suspend fun getBoolean(key: String, default: Boolean = false): Boolean
    suspend fun putBoolean(key: String, value: Boolean)

    suspend fun getString(key: String, default: String? = null): String?
    suspend fun putString(key: String, value: String?)
}
