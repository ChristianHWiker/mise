package io.github.chwi.recipecalculator.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 lands here — `IntegrityManagerFactory.create(context).requestIntegrityToken(...)`
 * with a fresh nonce on each call, parsing the verdict locally (soft-fail). Currently a stub
 * that returns [IntegrityVerdict.Trusted] so the portfolio flavor compiles without the
 * `com.google.android.play:integrity` dep.
 */
@Singleton
class PlayIntegrityChecker @Inject constructor() : IntegrityChecker {
    override suspend fun verify(): IntegrityVerdict = IntegrityVerdict.Trusted
}
