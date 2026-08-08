package dev.local.androidtools.offlinelab.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CircularBufferTest {
    @Test
    fun `evicts oldest once capacity exceeded`() {
        val buffer = CircularBuffer<Int>(3)
        buffer.add(1); buffer.add(2); buffer.add(3); buffer.add(4)
        assertEquals(listOf(2, 3, 4), buffer.snapshot())
    }

    @Test
    fun `clear empties buffer`() {
        val buffer = CircularBuffer<Int>(3)
        buffer.add(1)
        buffer.clear()
        assertEquals(emptyList<Int>(), buffer.snapshot())
    }
}
