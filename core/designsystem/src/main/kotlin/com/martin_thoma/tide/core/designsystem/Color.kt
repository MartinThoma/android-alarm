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

/** Every role is set, so no Material baseline (purple) colour shows up in dialogs or pickers. */
internal fun TideColors.toColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = sand,
            primaryContainer = shallow,
            onPrimaryContainer = onSand,
            inversePrimary = DayTideColors.sea,
            secondary = accent,
            onSecondary = sand,
            secondaryContainer = shallow,
            onSecondaryContainer = accent,
            tertiary = accent,
            onTertiary = sand,
            tertiaryContainer = Color(0xFF3A393E),
            onTertiaryContainer = onSand,
            background = sand,
            onBackground = onSand,
            surface = sand,
            onSurface = onSand,
            surfaceVariant = Color(0xFF3A393E),
            onSurfaceVariant = onSandVariant,
            surfaceTint = accent,
            inverseSurface = onSand,
            inverseOnSurface = sand,
            outline = Color(0xFF8E8B86),
            outlineVariant = Color(0xFF45444A),
            surfaceBright = Color(0xFF3A393E),
            surfaceDim = Color(0xFF1F1E21),
            surfaceContainerLowest = Color(0xFF1F1E21),
            surfaceContainerLow = Color(0xFF262529),
            surfaceContainer = navigationBar,
            surfaceContainerHigh = Color(0xFF333237),
            surfaceContainerHighest = Color(0xFF3A393E),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onSea,
            primaryContainer = shallow,
            onPrimaryContainer = onSand,
            inversePrimary = shallow,
            secondary = sea,
            onSecondary = onSea,
            secondaryContainer = sea,
            onSecondaryContainer = onSea,
            tertiary = sea,
            onTertiary = onSea,
            tertiaryContainer = wetSand,
            onTertiaryContainer = onSand,
            background = sand,
            onBackground = onSand,
            surface = sand,
            onSurface = onSand,
            surfaceVariant = wetSand,
            onSurfaceVariant = onSandVariant,
            surfaceTint = accent,
            inverseSurface = onSand,
            inverseOnSurface = sand,
            outline = Color(0xFF7C8486),
            outlineVariant = Color(0xFFCFC4B2),
            surfaceBright = Color(0xFFF6F0E6),
            surfaceDim = wetSand,
            surfaceContainerLowest = Color(0xFFF6F0E6),
            surfaceContainerLow = Color(0xFFF1E9DD),
            surfaceContainer = navigationBar,
            surfaceContainerHigh = Color(0xFFE9DFCF),
            surfaceContainerHighest = wetSand,
        )
    }
