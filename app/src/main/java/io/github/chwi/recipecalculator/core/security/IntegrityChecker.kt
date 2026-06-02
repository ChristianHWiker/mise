package io.github.chwi.recipecalculator.core.security

/**
 * Flavor seam for the Play Integrity check.
 *
 * In `play` this returns [IntegrityVerdict.Trusted] without making a network call — the
 * Integrity SDK isn't on the classpath at all. In `portfolio` the impl calls
 * `IntegrityManager.requestIntegrityToken(...)` and parses the verdict locally (soft-fail —
 * see the Phase 4 walkthrough for the threat-model discussion).
 *
 * Callers should treat anything other than [IntegrityVerdict.Trusted] as advisory and surface
 * it as a non-blocking warning, not a hard gate.
 */
interface IntegrityChecker {
    suspend fun verify(): IntegrityVerdict
}

sealed interface IntegrityVerdict {
    /** Device + app passed the check, or this build doesn't run the check. */
    data object Trusted : IntegrityVerdict
    /** Verdict came back negative. Reason is for logging/UI; do not branch on its content. */
    data class Untrusted(val reason: String) : IntegrityVerdict
    /** The check couldn't be performed (network, no Play Services, quota). Treat as advisory. */
    data object Unknown : IntegrityVerdict
}
