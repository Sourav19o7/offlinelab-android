package dev.local.androidtools.offlinelab

import android.content.Context
import dev.local.androidtools.offlinelab.internal.CircularBuffer
import dev.local.androidtools.offlinelab.internal.ProfileStore
import dev.local.androidtools.offlinelab.model.NetworkProfile
import dev.local.androidtools.offlinelab.model.SimulatedEvent

/** Receives every [SimulatedEvent] (including pass-throughs) as it happens. */
fun interface OfflineLabEventListener {
    fun onEvent(event: SimulatedEvent)
}

/**
 * OfflineLab's public entry point. Holds process-lifetime state (active profile, event history,
 * listener) behind a config that must be explicitly initialized — see [initialize].
 *
 * OfflineLab **never** intercepts traffic outside the [okhttp3.OkHttpClient] instances the host
 * app explicitly builds with [interceptor] added. There is no VPN, no root usage, no way for it
 * to see or affect any other app's network traffic.
 */
object OfflineLab {
    @Volatile
    private var config: OfflineLabConfig = OfflineLabConfig(enabled = false)

    @Volatile
    private var profileStore: ProfileStore = ProfileStore(NetworkProfile.Normal)

    @Volatile
    private var eventHistory: CircularBuffer<SimulatedEvent> = CircularBuffer(200)

    @Volatile
    private var listener: OfflineLabEventListener? = null

    /**
     * Must be called once, typically from `Application.onCreate()`, before building any
     * [okhttp3.OkHttpClient] that uses [interceptor]. Safe to call again to reconfigure.
     */
    fun initialize(context: Context, config: OfflineLabConfig) {
        this.config = config
        this.profileStore = ProfileStore(config.defaultProfile)
        this.eventHistory = CircularBuffer(config.maxEventHistory)
    }

    fun isEnabled(): Boolean = config.enabled

    /** The profile currently in effect. */
    fun currentProfile(): NetworkProfile = profileStore.get()

    /** Changes the active profile. Takes effect on the next intercepted request. */
    fun setProfile(profile: NetworkProfile) {
        profileStore.set(profile)
    }

    /** Resets to [NetworkProfile.Normal] — a convenience for a "reset to normal" UI action. */
    fun resetToNormal() {
        profileStore.set(NetworkProfile.Normal)
    }

    /** Registers a listener that's notified of every simulated (and pass-through) event. */
    fun setListener(listener: OfflineLabEventListener?) {
        this.listener = listener
    }

    /** The most recent events, oldest first, bounded by `OfflineLabConfig.maxEventHistory`. */
    fun eventHistory(): List<SimulatedEvent> = eventHistory.snapshot()

    fun clearEventHistory() {
        eventHistory.clear()
    }

    /**
     * Builds an [OfflineLabInterceptor] wired to this object's current config, active profile,
     * and event history/listener. [profileProvider] defaults to [currentProfile] but is exposed
     * as a parameter — matching the interceptor's constructor shape — so tests or advanced
     * callers can supply their own profile source without touching global state.
     */
    fun interceptor(profileProvider: () -> NetworkProfile = { currentProfile() }): OfflineLabInterceptor {
        return OfflineLabInterceptor(
            enabledProvider = { isEnabled() },
            profileProvider = profileProvider,
            allowListProvider = { config.allowList },
            denyListProvider = { config.denyList },
            onEvent = { event ->
                eventHistory.add(event)
                listener?.onEvent(event)
            },
        )
    }

    /** Test-only: fully resets OfflineLab to its uninitialized (disabled) state. */
    fun reset() {
        config = OfflineLabConfig(enabled = false)
        profileStore = ProfileStore(NetworkProfile.Normal)
        eventHistory = CircularBuffer(200)
        listener = null
    }
}
