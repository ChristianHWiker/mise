package io.github.chwi.recipecalculator.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * `portfolio` flavor Hilt bindings. Compiles only when building the portfolio variant —
 * the play flavor declares a same-named module in its own source set that binds no-ops.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    abstract fun bindAppLockController(impl: BiometricAppLockController): AppLockController

    @Binds
    abstract fun bindSecurePreferenceStore(impl: EncryptedSecurePreferenceStore): SecurePreferenceStore

    @Binds
    abstract fun bindIntegrityChecker(impl: PlayIntegrityChecker): IntegrityChecker

    @Binds
    abstract fun bindAppBackgroundObserver(impl: ProcessAppBackgroundObserver): AppBackgroundObserver
}
