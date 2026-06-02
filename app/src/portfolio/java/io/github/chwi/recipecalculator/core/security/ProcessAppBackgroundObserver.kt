package io.github.chwi.recipecalculator.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Portfolio: wires `ProcessLifecycleOwner` so we know when the entire app process moves to the
 * background (`ON_STOP` on the top-level lifecycle, distinct from any single Activity's
 * `onStop`). The lock re-arms on background so a sleeping phone re-prompts on next foreground.
 */
@Singleton
class ProcessAppBackgroundObserver @Inject constructor() : AppBackgroundObserver {

    private val started = AtomicBoolean(false)

    override fun start(onBackgrounded: () -> Unit) {
        if (!started.compareAndSet(false, true)) return
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                onBackgrounded()
            }
        })
    }
}
