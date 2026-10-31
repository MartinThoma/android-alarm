package com.martin_thoma.drift.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DurationFormatTest {
    @Test
    fun `minutes and seconds, hours only when needed`() {
        assertEquals("04:05", formatDuration(245_000, Locale.US))
        assertEquals("1:04:05", formatDuration(3_845_000, Locale.US))
        assertEquals("00:00", formatDuration(999, Locale.US))
    }

    @Test
    fun `hundredths use the language's decimal separator`() {
        assertEquals("04:05.27", formatDuration(245_279, Locale.US, hundredths = true))
        assertEquals("04:05,27", formatDuration(245_279, Locale.GERMANY, hundredths = true))
    }
}
