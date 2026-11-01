package com.martin_thoma.drift.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A turquoise lagoon seen from above. Water gets deeper (and darker) away from the shore:
 * [shallow] at the waterline, [shallowFar] where the shallows end, [sea] beyond, [deepSea]
 * at the top edge. Text on [sand] and the shallows uses [onSand]; text on the sea uses [onSea];
 * text on [GlassPane]s uses [onGlass]. By night the breaking water glows ([glow]).
 *
 * Contrast (WCAG AA, every text colour at least 4.5:1 on every ground it can meet): by day
 * the water is bright and clear, so all text is dark ink; by night all text is light.
 */
@Immutable
data class TideColors(
    val sand: Color,
    val wetSand: Color,
    val shallow: Color,
    val shallowFar: Color,
    val sea: Color,
    val deepSea: Color,
    val foam: Color,
    /** Bioluminescent light along the wave edges, or [Color.Transparent] by day. */
    val glow: Color,
    val glass: Color,
    val glassRim: Color,
    /** A stronger pane for the one action that should lead, e.g. snooze. */
    val glassRaised: Color,
    val glassRaisedRim: Color,
    val shadow: Color,
    val onSand: Color,
    val onSandVariant: Color,
    val onSea: Color,
    val onSeaVariant: Color,
    val onGlass: Color,
    val onGlassVariant: Color,
    val accent: Color,
    val onAccent: Color,
    /** Destructive actions, e.g. a row about to be deleted. */
    val danger: Color,
    val onDanger: Color,
    val dismissThumb: Color,
    val onDismissThumb: Color,
    val dismissThumbRim: Color,
    val navigationBar: Color,
)

val DayTideColors =
    TideColors(
        sand = Color(0xFFF0DCAC),
        wetSand = Color(0xFFE3C489),
        // Clear water over sand: almost sand-coloured at the shore, cyan further out.
        shallow = Color(0xFFD4EDC4),
        shallowFar = Color(0xFF8FE3D3),
        sea = Color(0xFF3CCACB),
        deepSea = Color(0xFF35B9CC),
        foam = Color(0xF2FFFFFF),
        glow = Color.Transparent,
        glass = Color(0xB8FFFFFF),
        glassRim = Color(0xCCFFFFFF),
        glassRaised = Color(0xD1FFFFFF),
        glassRaisedRim = Color(0xE6FFFFFF),
        shadow = Color(0x590B2F37),
        onSand = Color(0xFF0B2F37),
        onSandVariant = Color(0xFF1F4148),
        // The bright day water takes the same dark ink as the sand.
        onSea = Color(0xFF0B2F37),
        onSeaVariant = Color(0xFF1F4148),
        onGlass = Color(0xFF0B2F37),
        onGlassVariant = Color(0xFF1F4148),
        accent = Color(0xFF006A73),
        onAccent = Color(0xFFFFFFFF),
        danger = Color(0xFFC63D2F),
        onDanger = Color(0xFFFFFFFF),
        dismissThumb = Color(0xFF006A73),
        onDismissThumb = Color(0xFFFFFFFF),
        dismissThumbRim = Color.Transparent,
        navigationBar = Color(0xFFEAD3A0),
    )

val NightTideColors =
    TideColors(
        sand = Color(0xFF101418),
        wetSand = Color(0xFF0B0F13),
        // Bioluminescence: the breaking water near the shore glows blue, the open sea stays dark.
        shallow = Color(0xFF135EA6),
        shallowFar = Color(0xFF061A2A),
        sea = Color(0xFF04111B),
        deepSea = Color(0xFF02080D),
        foam = Color(0xF2A6ECFF),
        glow = Color(0xFF5FD4FF),
        glass = Color(0x990E1B26),
        glassRim = Color(0x335FD4FF),
        glassRaised = Color(0xCC12324A),
        glassRaisedRim = Color(0x8C5FD4FF),
        shadow = Color(0x99000000),
        onSand = Color(0xFFE3ECF0),
        onSandVariant = Color(0xFF9AABB3),
        onSea = Color(0xFFE3ECF0),
        // Bright enough for 4.5:1 over the glowing shallows, where the hero's last line sits (ContrastTest).
        onSeaVariant = Color(0xFFCCD9DF),
        onGlass = Color(0xFFE3ECF0),
        onGlassVariant = Color(0xFFA2B3BB),
        accent = Color(0xFF62D2FF),
        onAccent = Color(0xFF021827),
        danger = Color(0xFFC94A3E),
        onDanger = Color(0xFFFFFFFF),
        // Calmer than the snooze pane: a dark thumb with a glowing rim.
        dismissThumb = Color(0xFF0B2638),
        onDismissThumb = Color(0xFF5FD4FF),
        dismissThumbRim = Color(0x995FD4FF),
        navigationBar = Color(0xFF0E1216),
    )

val LocalTideColors = staticCompositionLocalOf { DayTideColors }

/** Every role is set, so no Material baseline (purple) colour shows up in dialogs or pickers. */
internal fun TideColors.toColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = Color(0xFF0E3550),
            onPrimaryContainer = Color(0xFFBFE9FF),
            inversePrimary = DayTideColors.accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = Color(0xFF10344D),
            onSecondaryContainer = Color(0xFFBFE9FF),
            tertiary = accent,
            onTertiary = onAccent,
            tertiaryContainer = Color(0xFF1D2A2D),
            onTertiaryContainer = onSand,
            background = sand,
            onBackground = onSand,
            surface = sand,
            onSurface = onSand,
            surfaceVariant = Color(0xFF223033),
            onSurfaceVariant = onSandVariant,
            surfaceTint = accent,
            inverseSurface = onSand,
            inverseOnSurface = sand,
            outline = Color(0xFF6D7F7E),
            outlineVariant = Color(0xFF2A3A3D),
            surfaceBright = Color(0xFF263236),
            surfaceDim = Color(0xFF0E1315),
            surfaceContainerLowest = Color(0xFF0E1315),
            surfaceContainerLow = Color(0xFF141A1D),
            surfaceContainer = navigationBar,
            surfaceContainerHigh = Color(0xFF1C2427),
            surfaceContainerHighest = Color(0xFF232D30),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = Color(0xFFBFEDEA),
            onPrimaryContainer = Color(0xFF0B2F37),
            inversePrimary = shallow,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = Color(0xFFBFEDEA),
            onSecondaryContainer = Color(0xFF0B2F37),
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
            outline = Color(0xFF75878A),
            outlineVariant = Color(0xFFD9C08C),
            surfaceBright = Color(0xFFFBF3E2),
            surfaceDim = wetSand,
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF8EDD5),
            surfaceContainer = navigationBar,
            surfaceContainerHigh = Color(0xFFFBF3E2),
            surfaceContainerHighest = wetSand,
        )
    }
