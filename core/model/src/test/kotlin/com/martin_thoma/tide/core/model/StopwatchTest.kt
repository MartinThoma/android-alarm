package com.martin_thoma.tide.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {
    @Test
    fun `runs, pauses and resumes from where it stopped`() {
        val running = Stopwatch().start(now = 1_000)
        assertEquals(2_500, running.elapsed(now = 3_500))

        val paused = running.pause(now = 3_500)
        assertEquals(2_500, paused.elapsed(now = 99_000))

        assertEquals(3_000, paused.start(now = 10_000).elapsed(now = 10_500))
    }

    @Test
    fun `starting twice or pausing twice changes nothing`() {
        val running = Stopwatch().start(1_000)
        assertEquals(running, running.start(5_000))
        val paused = running.pause(2_000)
        assertEquals(paused, paused.pause(9_000))
    }

    @Test
    fun `laps record split and total times`() {
        val watch =
            Stopwatch()
                .start(0)
                .lap(10_000)
                .lap(25_000)
                .pause(30_000)
                .start(40_000)
                .lap(42_000)
        assertEquals(
            listOf(Stopwatch.Lap(1, 10_000, 10_000), Stopwatch.Lap(2, 15_000, 25_000), Stopwatch.Lap(3, 7_000, 32_000)),
            watch.laps(),
        )
        assertEquals(watch.laps()[2] to watch.laps()[1], fastestAndSlowest(watch.laps()))
    }

    @Test
    fun `no lap while paused, and nothing to compare with a single lap`() {
        val watch =
            Stopwatch()
                .start(0)
                .lap(1_000)
                .pause(2_000)
                .lap(3_000)
        assertEquals(1, watch.laps().size)
        assertNull(fastestAndSlowest(watch.laps()))
    }

    @Test
    fun `reset clears everything`() {
        assertTrue(
            Stopwatch()
                .start(0)
                .lap(5)
                .reset()
                .isReset,
        )
    }
}
