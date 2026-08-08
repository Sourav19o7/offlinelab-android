package dev.local.androidtools.offlinelab

import org.junit.Assert.assertThrows
import org.junit.Test

class OfflineLabConfigTest {

    @Test
    fun `rejects non-positive maxEventHistory`() {
        assertThrows(IllegalArgumentException::class.java) {
            OfflineLabConfig(enabled = true, maxEventHistory = 0)
        }
    }

    @Test
    fun `custom profile rejects out-of-range rates`() {
        assertThrows(IllegalArgumentException::class.java) {
            dev.local.androidtools.offlinelab.model.NetworkProfile.Custom(failureRate = 1.5)
        }
    }
}
