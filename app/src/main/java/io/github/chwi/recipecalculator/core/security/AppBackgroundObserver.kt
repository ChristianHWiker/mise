package io.github.chwi.recipecalculator.core.security

/**
 * Flavor seam for "tell me when the whole app went to the background." The portfolio flavor
 * wires this to `ProcessLifecycleOwner` so the lock re-arms after backgrounding; the play
 * flavor doesn't need backgrounding hooks at all and binds a no-op that never fires.
 *
 * The observer is started once from MainActivity and lives for the process; the [onBackgrounded]
 * callback is invoked on the main thread.
 */
interface AppBackgroundObserver {
    /** Begin observing. Safe to call multiple times — only the first call wires the platform observer. */
    fun start(onBackgrounded: () -> Unit)
}
