package dev.local.androidtools.offlinelab.internal

import dev.local.androidtools.offlinelab.model.NetworkProfile
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe holder for the currently active profile. `AtomicReference` (rather than
 * `@Synchronized` get/set) is used because reads happen on every intercepted request — a lock-free
 * read is cheap and there's no compound operation here that needs a lock (get and set are each
 * already atomic).
 */
internal class ProfileStore(initial: NetworkProfile) {
    private val current = AtomicReference(initial)

    fun get(): NetworkProfile = current.get()

    fun set(profile: NetworkProfile) {
        current.set(profile)
    }
}
