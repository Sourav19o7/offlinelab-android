package dev.local.androidtools.offlinelab.internal

import kotlin.random.Random

/**
 * A single-method abstraction over "give me a double in [0, 1)", used instead of injecting
 * `kotlin.random.Random` directly. `kotlin.random.Random` only exposes `nextBits()` as its
 * abstract seam, which makes scripting an exact sequence of `nextDouble()` results in a test
 * awkward and dependent on its internal bit-to-double conversion. A one-method fun interface lets
 * tests provide an exact, readable sequence of rolls instead.
 */
fun interface RandomSource {
    fun nextDouble(): Double

    companion object {
        val system: RandomSource = RandomSource { Random.Default.nextDouble() }
    }
}
