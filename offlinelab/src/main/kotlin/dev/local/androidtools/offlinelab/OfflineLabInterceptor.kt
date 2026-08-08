package dev.local.androidtools.offlinelab

import dev.local.androidtools.offlinelab.internal.MatchEvaluator
import dev.local.androidtools.offlinelab.internal.ProfileResolver
import dev.local.androidtools.offlinelab.internal.RandomSource
import dev.local.androidtools.offlinelab.model.NetworkProfile
import dev.local.androidtools.offlinelab.model.RequestMatcher
import dev.local.androidtools.offlinelab.model.SimulatedEvent
import dev.local.androidtools.offlinelab.model.SimulationOutcome
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * The OkHttp [Interceptor] that actually applies network simulation. Public (not `internal`) so
 * it can be constructed and unit-tested directly, independent of the [OfflineLab] singleton —
 * see `OfflineLabInterceptorTest` for exactly this.
 *
 * Simulation only ever affects the host application's own [okhttp3.OkHttpClient] — this class
 * never touches sockets, VPN interfaces, or any traffic from other apps. When [enabledProvider]
 * returns `false`, [intercept] is a pure pass-through: no sleep, no random draw, no header added.
 */
class OfflineLabInterceptor(
    private val enabledProvider: () -> Boolean,
    private val profileProvider: () -> NetworkProfile,
    private val allowListProvider: () -> List<RequestMatcher> = { emptyList() },
    private val denyListProvider: () -> List<RequestMatcher> = { emptyList() },
    private val randomSource: RandomSource = RandomSource.system,
    private val onEvent: (SimulatedEvent) -> Unit = {},
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!enabledProvider()) {
            return chain.proceed(request)
        }

        val host = request.url.host
        val path = request.url.encodedPath
        val method = request.method

        val eligible = MatchEvaluator.isEligible(host, path, method, allowListProvider(), denyListProvider())
        val resolved = ProfileResolver.resolve(profileProvider())

        if (!eligible || resolved.name == "Normal") {
            recordEvent(request, resolved.name, SimulationOutcome.PASSED_THROUGH, appliedLatencyMs = 0)
            return chain.proceed(request)
        }

        if (resolved.offline) {
            recordEvent(request, resolved.name, SimulationOutcome.OFFLINE_FAILURE, appliedLatencyMs = 0)
            throw IOException("OfflineLab: simulated offline (profile=${resolved.name})")
        }

        if (resolved.latencyMs > 0) {
            Thread.sleep(resolved.latencyMs)
        }

        if (resolved.timeoutRate > 0.0 && randomSource.nextDouble() < resolved.timeoutRate) {
            recordEvent(request, resolved.name, SimulationOutcome.TIMEOUT, appliedLatencyMs = resolved.latencyMs)
            throw SocketTimeoutException("OfflineLab: simulated timeout (profile=${resolved.name})")
        }

        if (resolved.failureRate > 0.0 && randomSource.nextDouble() < resolved.failureRate) {
            recordEvent(request, resolved.name, SimulationOutcome.OFFLINE_FAILURE, appliedLatencyMs = resolved.latencyMs)
            throw IOException("OfflineLab: simulated connection failure (profile=${resolved.name})")
        }

        if (resolved.httpErrorRate > 0.0 && randomSource.nextDouble() < resolved.httpErrorRate) {
            recordEvent(
                request,
                resolved.name,
                SimulationOutcome.HTTP_ERROR,
                appliedLatencyMs = resolved.latencyMs,
                statusCode = resolved.httpErrorCode,
            )
            return simulatedErrorResponse(request, resolved.httpErrorCode)
        }

        return try {
            val response = chain.proceed(request)
            recordEvent(request, resolved.name, SimulationOutcome.PASSED_THROUGH, appliedLatencyMs = resolved.latencyMs)
            response
        } catch (e: IOException) {
            recordEvent(request, resolved.name, SimulationOutcome.REAL_ERROR, appliedLatencyMs = resolved.latencyMs)
            throw e
        }
    }

    private fun simulatedErrorResponse(request: okhttp3.Request, code: Int): Response {
        val body = "{\"offlinelab_simulated\":true,\"status\":$code}".toResponseBody("application/json".toMediaType())
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OfflineLab simulated response")
            .header("X-OfflineLab-Simulated", "true")
            .body(body)
            .build()
    }

    private fun recordEvent(
        request: okhttp3.Request,
        profileName: String,
        outcome: SimulationOutcome,
        appliedLatencyMs: Long,
        statusCode: Int? = null,
    ) {
        onEvent(
            SimulatedEvent(
                timestampMs = System.currentTimeMillis(),
                method = request.method,
                url = request.url.toString(),
                profileName = profileName,
                outcome = outcome,
                simulatedStatusCode = statusCode,
                appliedLatencyMs = appliedLatencyMs,
            ),
        )
    }
}
