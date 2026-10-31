package com.martin_thoma.drift.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Soft enough to read on, sharp enough that the shoreline still drifts through the glass.
private val BLUR_RADIUS = 12.dp

/**
 * A pane of frosted glass over the beach: it blurs the water and sand of the nearest
 * [TideSurface] behind it, so the tide stays visible while text on it stays readable
 * wherever the water is. Content gets [TideColors.onGlass]. A [raised] pane is the one
 * action a screen leads with.
 */
@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    raised: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalTideColors.current
    val tide = LocalTideState.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val tint = if (raised) colors.glassRaised else colors.glass
    val rim = if (raised) colors.glassRaisedRim else colors.glassRim

    Box(
        modifier =
            modifier
                .shadow(elevation = 10.dp, shape = shape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .clip(shape)
                .onGloballyPositioned { pane ->
                    val surface = tide?.coordinates
                    if (surface != null && surface.isAttached && pane.isAttached) origin = surface.localPositionOf(pane, Offset.Zero)
                }.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        if (tide != null) {
            // The beach behind the pane, drawn again at its place and blurred.
            Canvas(
                Modifier.matchParentSize().graphicsLayer {
                    val radius = BLUR_RADIUS.toPx()
                    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                },
            ) {
                translate(-origin.x, -origin.y) { drawTide(colors, tide.level.value, tide.motion, tide.size) }
            }
        }
        // Light catches the upper edge of the glass a little more.
        val sheen = tint.copy(alpha = (tint.alpha + 0.14f).coerceAtMost(1f))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(sheen, tint))).border(1.dp, rim, shape))
        CompositionLocalProvider(LocalContentColor provides colors.onGlass) { content() }
    }
}
