package com.martin_thoma.drift.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Theme of direction "D · Tide" (docs/requirements.md, section 7.1). */
@Composable
fun TideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tideColors = if (darkTheme) NightTideColors else DayTideColors
    CompositionLocalProvider(LocalTideColors provides tideColors) {
        MaterialTheme(
            colorScheme = tideColors.toColorScheme(darkTheme),
            typography = TideTypography,
            content = content,
        )
    }
}
