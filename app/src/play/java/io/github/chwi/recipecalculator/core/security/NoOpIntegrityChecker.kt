package io.github.chwi.recipecalculator.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * `play` flavor: the Play Integrity SDK isn't on the classpath. Always returns
 * [IntegrityVerdict.Trusted] so launch-time check callers don't need a flavor branch.
 */
@Singleton
class NoOpIntegrityChecker @Inject constructor() : IntegrityChecker {
    override suspend fun verify(): IntegrityVerdict = IntegrityVerdict.Trusted
}
