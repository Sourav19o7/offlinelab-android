package dev.local.androidtools.offlinelab.internal

import dev.local.androidtools.offlinelab.model.NetworkProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileResolverTest {

    @Test
    fun `Normal has no latency and no failure rates`() {
        val resolved = ProfileResolver.resolve(NetworkProfile.Normal)
        assertEquals(0L, resolved.latencyMs)
        assertEquals(0.0, resolved.failureRate, 0.0)
        assertEquals(0.0, resolved.timeoutRate, 0.0)
        assertEquals(0.0, resolved.httpErrorRate, 0.0)
        assertFalse(resolved.offline)
    }

    @Test
    fun `Offline is flagged offline with no other rates needed`() {
        assertTrue(ProfileResolver.resolve(NetworkProfile.Offline).offline)
    }

    @Test
    fun `RateLimited always returns 429`() {
        val resolved = ProfileResolver.resolve(NetworkProfile.RateLimited)
        assertEquals(1.0, resolved.httpErrorRate, 0.0)
        assertEquals(429, resolved.httpErrorCode)
    }

    @Test
    fun `Custom passes through given parameters unchanged`() {
        val custom = NetworkProfile.Custom(latencyMs = 1500, failureRate = 0.25, timeoutRate = 0.10, httpErrorRate = 0.15)
        val resolved = ProfileResolver.resolve(custom)

        assertEquals(1500L, resolved.latencyMs)
        assertEquals(0.25, resolved.failureRate, 0.0)
        assertEquals(0.10, resolved.timeoutRate, 0.0)
        assertEquals(0.15, resolved.httpErrorRate, 0.0)
    }
}
