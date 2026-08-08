package dev.local.androidtools.offlinelab.model

enum class SimulationOutcome { PASSED_THROUGH, OFFLINE_FAILURE, TIMEOUT, HTTP_ERROR, REAL_ERROR }

/**
 * A record of what OfflineLab did (or deliberately didn't do) for one request. Recorded for every
 * matched request, including [SimulationOutcome.PASSED_THROUGH] ones, so the debug panel can show
 * "this one was real" as clearly as "this one was simulated".
 */
data class SimulatedEvent(
    val timestampMs: Long,
    val method: String,
    val url: String,
    val profileName: String,
    val outcome: SimulationOutcome,
    val simulatedStatusCode: Int? = null,
    val appliedLatencyMs: Long = 0,
)
