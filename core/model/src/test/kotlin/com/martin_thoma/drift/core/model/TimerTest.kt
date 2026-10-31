package com.martin_thoma.drift.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTest {
    private val tenMinutes = Timer(id = 1, setMillis = 600_000)

    @Test
    fun `counts down while running and keeps its time while paused`() {
        val running = tenMinutes.start(now = 1_000)
        assertEquals(540_000, running.remaining(now = 61_000))
        val paused = running.pause(now = 61_000)
        assertEquals(540_000, paused.remaining(now = 999_999))
        assertEquals(30_000, paused.start(now = 2_000_000).remaining(now = 2_510_000))
    }

    @Test
    fun `overtime is negative and progress stops at the end`() {
        val running = tenMinutes.start(now = 0)
        assertTrue(running.hasExpired(now = 612_000))
        assertEquals(-12_000, running.remaining(now = 612_000))
        assertEquals(1f, running.progress(now = 612_000))
        assertEquals(0.5f, running.progress(now = 300_000))
    }

    @Test
    fun `plus one minute extends a run, and restarts one that has ended`() {
        assertEquals(660_000, tenMinutes.addMinute(now = 0).remaining(now = 0))
        assertEquals(120_000, tenMinutes.start(0).addMinute(now = 540_000).remaining(now = 540_000))
        val restarted = tenMinutes.start(0).addMinute(now = 700_000)
        assertEquals(60_000, restarted.remaining(now = 700_000))
        assertFalse(restarted.hasExpired(now = 700_000))
    }

    @Test
    fun `reset returns to the set duration`() {
        val reset =
            tenMinutes
                .start(0)
                .addMinute(10)
                .pause(100_000)
                .reset()
        assertTrue(reset.isIdle)
        assertEquals(600_000, reset.remaining(now = 5))
    }

    @Test
    fun `digits fill from the right like a microwave`() {
        assertEquals(600_000, Timer.durationFromDigits("1000"))
        assertEquals(5_400_000, Timer.durationFromDigits("13000"))
        assertEquals(90_000, Timer.durationFromDigits("90"))
        assertEquals(0, Timer.durationFromDigits(""))
    }
}
