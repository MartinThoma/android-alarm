package com.martin_thoma.drift.feature.ringing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeCounterTest {
    private val millis = 1_000_000L

    @Test
    fun `a phone at rest does not shake`() {
        val counter = ShakeCounter()
        repeat(100) { assertFalse(counter.onReading(0f, 0f, 9.81f, it * 20 * millis)) }
        assertEquals(0, counter.shakes)
    }

    @Test
    fun `a firm jolt counts as a shake`() {
        val counter = ShakeCounter()
        assertTrue(counter.onReading(15f, 10f, 9.81f, 0))
        assertEquals(1, counter.shakes)
    }

    @Test
    fun `jolts within a quarter of a second are one shake`() {
        val counter = ShakeCounter()
        counter.onReading(25f, 0f, 0f, 0)
        assertFalse(counter.onReading(25f, 0f, 0f, 100 * millis))
        assertTrue(counter.onReading(25f, 0f, 0f, 260 * millis))
        assertEquals(2, counter.shakes)
    }

    @Test
    fun `counting goes on from where it was`() {
        val counter = ShakeCounter(shakes = 7)
        counter.onReading(25f, 0f, 0f, 0)
        assertEquals(8, counter.shakes)
    }
}
