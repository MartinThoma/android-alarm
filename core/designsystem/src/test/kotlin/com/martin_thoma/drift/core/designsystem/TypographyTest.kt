package com.martin_thoma.drift.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression test: hundredths in a lighter style without tabular figures made the stopwatch jitter (S25, 2026-10-31). */
class TypographyTest {
    @Test
    fun `every text style has tabular figures, so counting digits never move`() {
        val styles =
            with(TideTypography) {
                listOf(
                    displayLarge,
                    displayMedium,
                    displaySmall,
                    headlineLarge,
                    headlineMedium,
                    headlineSmall,
                    titleLarge,
                    titleMedium,
                    titleSmall,
                    bodyLarge,
                    bodyMedium,
                    bodySmall,
                    labelLarge,
                    labelMedium,
                    labelSmall,
                )
            }
        styles.forEach { assertEquals("tnum", it.fontFeatureSettings) }
    }
}
