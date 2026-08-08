package dev.local.androidtools.offlinelab

import dev.local.androidtools.offlinelab.internal.RandomSource
import dev.local.androidtools.offlinelab.model.NetworkProfile
import dev.local.androidtools.offlinelab.model.SimulatedEvent
import dev.local.androidtools.offlinelab.model.SimulationOutcome
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** A deterministic sequence of `nextDouble()` results, so tests can force a specific branch. */
private class ScriptedRandomSource(private val rolls: List<Double>) : RandomSource {
    private var index = 0
    override fun nextDouble(): Double = rolls[index++]
}

/** Minimal fake [Interceptor.Chain]: only `request()`/`proceed()` are ever called by [OfflineLabInterceptor]. */
private class FakeChain(private val request: Request) : Interceptor.Chain {
    var didProceed = false
        private set

    override fun request(): Request = request

    override fun proceed(request: Request): Response {
        didProceed = true
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("real-body".toResponseBody(null))
            .build()
    }

    override fun connection(): Connection? = null
    override fun call(): Call = throw UnsupportedOperationException("not used by OfflineLabInterceptor")
    override fun connectTimeoutMillis(): Int = 0
    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun readTimeoutMillis(): Int = 0
    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun writeTimeoutMillis(): Int = 0
    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}

class OfflineLabInterceptorTest {

    private fun request(url: String = "https://api.example.com/pay", method: String = "GET") =
        Request.Builder().url(url).method(method, null).build()

    @Test
    fun `Normal profile passes every request through untouched`() {
        val events = mutableListOf<SimulatedEvent>()
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { NetworkProfile.Normal },
            randomSource = ScriptedRandomSource(emptyList()),
            onEvent = { events += it },
        )
        val chain = FakeChain(request())

        val response = interceptor.intercept(chain)

        assertTrue(chain.didProceed)
        assertEquals(200, response.code)
        assertEquals(SimulationOutcome.PASSED_THROUGH, events.single().outcome)
    }

    @Test
    fun `disabled interceptor never touches the request regardless of profile`() {
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { false },
            profileProvider = { NetworkProfile.Offline }, // would normally always throw
        )
        val chain = FakeChain(request())

        val response = interceptor.intercept(chain)

        assertTrue(chain.didProceed)
        assertEquals(200, response.code)
    }

    @Test
    fun `Offline profile always fails without calling proceed`() {
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { NetworkProfile.Offline },
        )
        val chain = FakeChain(request())

        try {
            interceptor.intercept(chain)
            org.junit.Assert.fail("expected IOException")
        } catch (e: IOException) {
            assertFalse(chain.didProceed)
        }
    }

    @Test
    fun `scripted random source below timeoutRate throws SocketTimeoutException`() {
        val custom = NetworkProfile.Custom(latencyMs = 0, timeoutRate = 0.5, failureRate = 0.0, httpErrorRate = 0.0)
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { custom },
            randomSource = ScriptedRandomSource(listOf(0.1)), // 0.1 < 0.5 -> timeout triggers
        )
        val chain = FakeChain(request())

        try {
            interceptor.intercept(chain)
            org.junit.Assert.fail("expected SocketTimeoutException")
        } catch (e: SocketTimeoutException) {
            assertFalse(chain.didProceed)
        }
    }

    @Test
    fun `scripted random source above all rates falls through to a real request`() {
        val custom = NetworkProfile.Custom(latencyMs = 0, timeoutRate = 0.1, failureRate = 0.1, httpErrorRate = 0.1)
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { custom },
            // three rolls consumed (timeout, failure, httpError), all above their thresholds
            randomSource = ScriptedRandomSource(listOf(0.9, 0.9, 0.9)),
        )
        val chain = FakeChain(request())

        val response = interceptor.intercept(chain)

        assertTrue(chain.didProceed)
        assertEquals(200, response.code)
    }

    @Test
    fun `httpErrorRate below threshold returns a simulated response marked with a header`() {
        val custom = NetworkProfile.Custom(latencyMs = 0, timeoutRate = 0.0, failureRate = 0.0, httpErrorRate = 0.5, httpErrorCode = 503)
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { custom },
            randomSource = ScriptedRandomSource(listOf(0.1)),
        )
        val chain = FakeChain(request())

        val response = interceptor.intercept(chain)

        assertFalse(chain.didProceed)
        assertEquals(503, response.code)
        assertEquals("true", response.header("X-OfflineLab-Simulated"))
    }

    @Test
    fun `requests outside the allow list are never simulated`() {
        val interceptor = OfflineLabInterceptor(
            enabledProvider = { true },
            profileProvider = { NetworkProfile.Offline },
            allowListProvider = { listOf(dev.local.androidtools.offlinelab.model.RequestMatcher(host = "other.example.com")) },
        )
        val chain = FakeChain(request(url = "https://api.example.com/pay"))

        val response = interceptor.intercept(chain)

        assertTrue(chain.didProceed)
        assertEquals(200, response.code)
    }
}
