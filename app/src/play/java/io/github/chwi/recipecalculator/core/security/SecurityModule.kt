package io.github.chwi.recipecalculator.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * `play` flavor Hilt bindings for the security seams. The `portfolio` flavor declares a
 * module with the same name in its own source set — only one is compiled per variant, so
 * there's no duplicate-binding conflict.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    abstract fun bindAppLockController(impl: NoOpAppLockController): AppLockController

    @Binds
    abstract fun bindSecurePreferenceStore(impl: NoOpSecurePreferenceStore): SecurePreferenceStore

    @Binds
    abstract fun bindIntegrityChecker(impl: NoOpIntegrityChecker): IntegrityChecker
}
