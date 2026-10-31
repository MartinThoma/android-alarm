package com.martin_thoma.tide.core.designsystem

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

/** Tabular figures keep digits from jumping while a time counts. */
private fun TextStyle.figtree(numerals: Boolean = false) = copy(fontFamily = Figtree, fontFeatureSettings = if (numerals) "tnum" else null)

/** Display styles are for times: light weight, tabular figures. */
private fun TextStyle.time() = figtree(numerals = true).copy(fontWeight = FontWeight.Light)

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
