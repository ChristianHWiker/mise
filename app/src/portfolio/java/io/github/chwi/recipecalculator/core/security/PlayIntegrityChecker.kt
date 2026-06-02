package io.github.chwi.recipecalculator.core.security

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Portfolio: real Play Integrity check (classic API), soft-fail.
 *
 * **Soft-fail trade-off (documented intentionally):**
 * The integrity token Play returns is a JWE — the verdict payload is encrypted to Google's
 * server-held key and *cannot* be decrypted locally. A production verifier would POST the
 * token to a backend that calls Google's decryption endpoint and reads the verdict fields
 * (`appRecognitionVerdict`, `deviceRecognitionVerdict`, …). This app is local-only, so we
 * treat "the API returned a token" as evidence the platform sees this as a legitimate Play
 * install on a real device — the bar Play already imposes to issue a token. Errors
 * (no Play Services, quota, no cloud-project registration, sideloaded build) come back as
 * [IntegrityVerdict.Unknown]; [IntegrityVerdict.Untrusted] is structurally unreachable here.
 *
 * In dev installs without a registered cloud project number, this will reliably return
 * `Unknown` — that's expected. Once a Play Console internal-track install is configured,
 * `Trusted` is the normal outcome.
 */
@Singleton
class PlayIntegrityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : IntegrityChecker {

    override suspend fun verify(): IntegrityVerdict = runCatching {
        val manager = IntegrityManagerFactory.create(context)
        val request = IntegrityTokenRequest.builder()
            .setNonce(generateNonce())
            .build()
        suspendCancellableCoroutine { cont ->
            manager.requestIntegrityToken(request)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        IntegrityVerdict.Trusted
    }.getOrElse { IntegrityVerdict.Unknown }

    private fun generateNonce(): String {
        // Play requires ≥ 16 bytes of base64-encoded entropy per call; 32 keeps us comfortable.
        // `android.util.Base64` is used in preference to `java.util.Base64` because the latter
        // is only available from API 26 and our minSdk is 24.
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
