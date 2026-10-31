package com.martin_thoma.tide.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The beach palette of direction "D · Tide": sand, and sea in three depths.
 * Text on [sand] uses [onSand]; text on [sea] uses [onSea].
 */
@Immutable
data class TideColors(
    val sand: Color,
    val wetSand: Color,
    val shallow: Color,
    val sea: Color,
    val foam: Color,
    val onSand: Color,
    val onSandVariant: Color,
    val onSea: Color,
    val onSeaVariant: Color,
    val accent: Color,
    val navigationBar: Color,
)

val DayTideColors =
    TideColors(
        sand = Color(0xFFEDE4D6),
        wetSand = Color(0xFFE1D5C2),
        shallow = Color(0xFF8DB7B5),
        sea = Color(0xFF3F717A),
        foam = Color(0x8CFFFFFF),
        onSand = Color(0xFF233238),
        onSandVariant = Color(0xFF4F585C),
        onSea = Color(0xFFF7F4EE),
        onSeaVariant = Color(0xFFE3EEEE),
        accent = Color(0xFF3F717A),
        navigationBar = Color(0xFFE5DAC8),
    )

val NightTideColors =
    TideColors(
        sand = Color(0xFF2C2B2E),
        wetSand = Color(0xFF262629),
        shallow = Color(0xFF2A4550),
        sea = Color(0xFF1C3340),
        foam = Color(0x38E6E3DC),
        onSand = Color(0xFFECE8E1),
        onSandVariant = Color(0xFFA9A6A0),
        onSea = Color(0xFFECE8E1),
        onSeaVariant = Color(0xFFA9A6A0),
        accent = Color(0xFF8FB9B6),
        navigationBar = Color(0xFF242327),
    )

val LocalTideColors = staticCompositionLocalOf { DayTideColors }

internal fun TideColors.toColorScheme(dark: Boolean): ColorScheme {
    val onAccent = if (dark) sand else onSea
    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondaryContainer = shallow,
            onSecondaryContainer = accent,
            background = sand,
            onBackground = onSand,
            surface = sand,
            onSurface = onSand,
            onSurfaceVariant = onSandVariant,
            surfaceContainer = navigationBar,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondaryContainer = sea,
            onSecondaryContainer = onSea,
            background = sand,
            onBackground = onSand,
            surface = sand,
            onSurface = onSand,
            onSurfaceVariant = onSandVariant,
            surfaceContainer = navigationBar,
        )
    }
}
