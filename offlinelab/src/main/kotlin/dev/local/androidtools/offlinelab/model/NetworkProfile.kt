package dev.local.androidtools.offlinelab.model

/**
 * A named network condition. Built-in profiles resolve to concrete parameters via
 * `internal.ProfileResolver` — see that file for the exact numbers each one maps to and why.
 * [Custom] lets you specify those parameters directly instead of picking a preset.
 */
sealed class NetworkProfile {
    /** No simulation: every request passes through untouched. This is the safe default. */
    data object Normal : NetworkProfile()

    /** Every matched request fails immediately with a simulated `IOException`, before any real network call. */
    data object Offline : NetworkProfile()

    /** Adds fixed latency typical of a poor 3G connection. Requests still succeed. */
    data object Slow3G : NetworkProfile()

    /** Adds heavier fixed latency than [Slow3G], with no failures. */
    data object HighLatency : NetworkProfile()

    /** A portion of requests simulate a client-side timeout instead of completing. */
    data object RandomTimeout : NetworkProfile()

    /** A mix of latency, connection failures, timeouts, and HTTP errors — an unreliable network. */
    data object Flaky : NetworkProfile()

    /** A portion of requests return a simulated 5xx response instead of reaching the real server. */
    data object ServerErrors : NetworkProfile()

    /** Every matched request returns a simulated `429 Too Many Requests`. */
    data object RateLimited : NetworkProfile()

    /**
     * Fully custom simulation parameters.
     *
     * @param latencyMs extra delay added before any other simulation logic runs.
     * @param failureRate probability (0.0–1.0) a request fails with a simulated `IOException`.
     * @param timeoutRate probability (0.0–1.0) a request fails with a simulated timeout.
     * @param httpErrorRate probability (0.0–1.0) a request returns [httpErrorCode] instead of the real response.
     * @param httpErrorCode the status code used when [httpErrorRate] triggers.
     */
    data class Custom(
        val latencyMs: Long = 0,
        val failureRate: Double = 0.0,
        val timeoutRate: Double = 0.0,
        val httpErrorRate: Double = 0.0,
        val httpErrorCode: Int = 500,
    ) : NetworkProfile() {
        init {
            require(latencyMs >= 0) { "latencyMs must be >= 0" }
            require(failureRate in 0.0..1.0) { "failureRate must be in 0.0..1.0" }
            require(timeoutRate in 0.0..1.0) { "timeoutRate must be in 0.0..1.0" }
            require(httpErrorRate in 0.0..1.0) { "httpErrorRate must be in 0.0..1.0" }
        }
    }
}
