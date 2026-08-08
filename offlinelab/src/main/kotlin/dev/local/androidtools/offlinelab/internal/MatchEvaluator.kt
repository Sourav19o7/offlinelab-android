package dev.local.androidtools.offlinelab.internal

import dev.local.androidtools.offlinelab.model.RequestMatcher

/**
 * Decides whether a request is eligible for simulation. Deny always wins over allow, and an empty
 * [allowList] means "everything is eligible" (the common case — most apps don't need to scope
 * simulation to specific endpoints).
 */
internal object MatchEvaluator {
    fun isEligible(
        host: String,
        path: String,
        method: String,
        allowList: List<RequestMatcher>,
        denyList: List<RequestMatcher>,
    ): Boolean {
        if (denyList.any { it.matches(host, path, method) }) return false
        if (allowList.isEmpty()) return true
        return allowList.any { it.matches(host, path, method) }
    }
}
