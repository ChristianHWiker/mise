package io.github.chwi.recipecalculator.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * `play` flavor: encryption is compiled out. Reads return the supplied default, writes are
 * dropped. Production code in play should never reach this — every call site is gated by a
 * portfolio-only feature — so silent no-ops are safer than throwing.
 */
@Singleton
class NoOpSecurePreferenceStore @Inject constructor() : SecurePreferenceStore {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean = default
    override suspend fun putBoolean(key: String, value: Boolean) = Unit
    override suspend fun getString(key: String, default: String?): String? = default
    override suspend fun putString(key: String, value: String?) = Unit
}
