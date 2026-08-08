package dev.local.androidtools.offlinelab

import dev.local.androidtools.offlinelab.model.NetworkProfile
import dev.local.androidtools.offlinelab.model.RequestMatcher

/**
 * Configuration for [OfflineLab]. As with every debug-only tool in this project, [enabled] must
 * be wired to the host app's own debug flag — OfflineLab cannot detect the build type on its own.
 * When `enabled` is `false`, [OfflineLab.interceptor] returns an interceptor that is a pure
 * pass-through: it never sleeps, never fails a request, never touches [defaultProfile] at all.
 *
 * @param allowList if non-empty, only requests matching at least one [RequestMatcher] here are
 *   candidates for simulation; everything else always passes through untouched.
 * @param denyList requests matching any matcher here always pass through untouched, regardless
 *   of [allowList] or the active profile. Deny takes precedence over allow.
 */
data class OfflineLabConfig(
    val enabled: Boolean,
    val defaultProfile: NetworkProfile = NetworkProfile.Normal,
    val allowList: List<RequestMatcher> = emptyList(),
    val denyList: List<RequestMatcher> = emptyList(),
    val maxEventHistory: Int = 200,
) {
    init {
        require(maxEventHistory > 0) { "maxEventHistory must be positive" }
    }
}
