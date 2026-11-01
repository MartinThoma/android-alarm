package com.martin_thoma.drift.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Text and controls stand out from what is behind them (WCAG 1.4.3 and 1.4.11, as Android's
 * accessibility guide asks): 4.5:1 for text, 3:1 for icons and the outlines of controls.
 * Glass is see-through, so it counts over every surface it can lie on.
 */
class ContrastTest {
    private fun ratio(
        foreground: Color,
        background: Color,
    ): Double {
        val a = foreground.compositeOver(background).luminance() + 0.05
        val b = background.luminance() + 0.05
        return max(a, b) / min(a, b)
    }

    private fun failures(
        colors: TideColors,
        name: String,
    ): List<String> {
        val water = listOf(colors.sea, colors.deepSea, colors.shallow, colors.shallowFar)
        val glassOn = (water + colors.sand + colors.wetSand).map { colors.glass.compositeOver(it) }
        val raisedOn = (water + colors.sand + colors.wetSand).map { colors.glassRaised.compositeOver(it) }
        val checks =
            buildList {
                for (glass in glassOn + raisedOn) {
                    add(Triple("onGlass on glass", ratio(colors.onGlass, glass), 4.5))
                    add(Triple("onGlassVariant on glass", ratio(colors.onGlassVariant, glass), 4.5))
                    add(Triple("accent text on glass", ratio(colors.accent, glass), 4.5))
                }
                for (sand in listOf(colors.sand, colors.wetSand, colors.navigationBar)) {
                    add(Triple("onSand on sand", ratio(colors.onSand, sand), 4.5))
                    add(Triple("onSandVariant on sand", ratio(colors.onSandVariant, sand), 4.5))
                }
                for (sea in water) {
                    add(Triple("onSea on water", ratio(colors.onSea, sea), 4.5))
                    add(Triple("onSeaVariant on water", ratio(colors.onSeaVariant, sea), 4.5))
                }
                add(Triple("onAccent on accent", ratio(colors.onAccent, colors.accent), 4.5))
                add(Triple("onDanger on danger", ratio(colors.onDanger, colors.danger), 4.5))
                add(Triple("onDismissThumb on dismissThumb", ratio(colors.onDismissThumb, colors.dismissThumb), 3.0))
                // A filled button against the sand it sits on.
                add(Triple("accent button on sand", ratio(colors.accent, colors.sand), 3.0))
            }
        return checks.filter { it.second < it.third }.map { (what, r, min) -> "$name: $what %.2f < $min".format(r) }.distinct()
    }

    @Test
    fun `day colours have enough contrast`() {
        val failures = failures(DayTideColors, "day")
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `night colours have enough contrast`() {
        val failures = failures(NightTideColors, "night")
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
