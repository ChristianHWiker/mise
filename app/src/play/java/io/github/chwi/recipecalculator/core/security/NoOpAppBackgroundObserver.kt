package io.github.chwi.recipecalculator.core.security

import javax.inject.Inject
import javax.inject.Singleton

/** Play flavor: the lock isn't a feature so the background hook is unused. */
@Singleton
class NoOpAppBackgroundObserver @Inject constructor() : AppBackgroundObserver {
    override fun start(onBackgrounded: () -> Unit) = Unit
}
