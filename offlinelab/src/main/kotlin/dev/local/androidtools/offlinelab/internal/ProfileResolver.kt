package dev.local.androidtools.offlinelab.internal

import dev.local.androidtools.offlinelab.model.NetworkProfile

/** Concrete, resolved simulation parameters — what every built-in [NetworkProfile] boils down to. */
internal data class ResolvedProfile(
    val name: String,
    val offline: Boolean,
    val latencyMs: Long,
    val failureRate: Double,
    val timeoutRate: Double,
    val httpErrorRate: Double,
    val httpErrorCode: Int,
)

/**
 * Maps every [NetworkProfile] to a [ResolvedProfile]. Kept as a single pure function (no state,
 * no I/O) so it's trivial to unit test exhaustively and trivial to read as the one source of
 * truth for "what does Flaky actually do".
 */
internal object ProfileResolver {
    fun resolve(profile: NetworkProfile): ResolvedProfile = when (profile) {
        is NetworkProfile.Normal -> ResolvedProfile("Normal", offline = false, latencyMs = 0, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 0.0, httpErrorCode = 0)
        is NetworkProfile.Offline -> ResolvedProfile("Offline", offline = true, latencyMs = 0, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 0.0, httpErrorCode = 0)
        is NetworkProfile.Slow3G -> ResolvedProfile("Slow3G", offline = false, latencyMs = 1400, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 0.0, httpErrorCode = 0)
        is NetworkProfile.HighLatency -> ResolvedProfile("HighLatency", offline = false, latencyMs = 3000, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 0.0, httpErrorCode = 0)
        is NetworkProfile.RandomTimeout -> ResolvedProfile("RandomTimeout", offline = false, latencyMs = 0, failureRate = 0.0, timeoutRate = 0.3, httpErrorRate = 0.0, httpErrorCode = 0)
        is NetworkProfile.Flaky -> ResolvedProfile("Flaky", offline = false, latencyMs = 200, failureRate = 0.3, timeoutRate = 0.1, httpErrorRate = 0.1, httpErrorCode = 503)
        is NetworkProfile.ServerErrors -> ResolvedProfile("ServerErrors", offline = false, latencyMs = 0, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 0.5, httpErrorCode = 500)
        is NetworkProfile.RateLimited -> ResolvedProfile("RateLimited", offline = false, latencyMs = 0, failureRate = 0.0, timeoutRate = 0.0, httpErrorRate = 1.0, httpErrorCode = 429)
        is NetworkProfile.Custom -> ResolvedProfile(
            name = "Custom",
            offline = false,
            latencyMs = profile.latencyMs,
            failureRate = profile.failureRate,
            timeoutRate = profile.timeoutRate,
            httpErrorRate = profile.httpErrorRate,
            httpErrorCode = profile.httpErrorCode,
        )
    }
}
