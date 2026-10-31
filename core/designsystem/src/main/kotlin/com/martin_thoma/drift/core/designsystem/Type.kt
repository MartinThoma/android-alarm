package com.martin_thoma.drift.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

private fun figtree(weight: FontWeight) =
    Font(
        resId = R.font.figtree,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

/** Figtree (SIL OFL 1.1), bundled because the app has no network access. */
val Figtree =
    FontFamily(
        figtree(FontWeight.Light),
        figtree(FontWeight.Normal),
        figtree(FontWeight.Medium),
        figtree(FontWeight.SemiBold),
    )

/**
 * Every style uses tabular figures: all digits are equally wide, so a counting time never
 * jitters, whatever style it is set in (hundredths, lap times, countdowns).
 */
private fun TextStyle.figtree() = copy(fontFamily = Figtree, fontFeatureSettings = "tnum")

/** Display styles are for times: light weight. */
private fun TextStyle.time() = figtree().copy(fontWeight = FontWeight.Light)

internal val TideTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.time(),
            displayMedium = displayMedium.time(),
            displaySmall = displaySmall.time(),
            headlineLarge = headlineLarge.figtree(),
            headlineMedium = headlineMedium.figtree(),
            headlineSmall = headlineSmall.figtree(),
            titleLarge = titleLarge.figtree(),
            titleMedium = titleMedium.figtree(),
            titleSmall = titleSmall.figtree(),
            bodyLarge = bodyLarge.figtree(),
            bodyMedium = bodyMedium.figtree(),
            bodySmall = bodySmall.figtree(),
            labelLarge = labelLarge.figtree(),
            labelMedium = labelMedium.figtree(),
            labelSmall = labelSmall.figtree(),
        )
    }
