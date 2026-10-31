package com.martin_thoma.drift.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Share of the row's width a swipe must cover before letting go deletes the alarm. */
private const val DELETE_THRESHOLD = 0.5f

/**
 * Swiping a row towards the start deletes it, once it has moved past [DELETE_THRESHOLD] of its
 * width; short of that it springs back, however fast the swipe. Material's SwipeToDismissBox
 * deleted after 56 dp, and for any swipe faster than 125 dp/s (Martin, on the S25).
 * Used for alarms and timers alike.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalTideColors.current
    val haptics = LocalHapticFeedback.current
    var width by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    // Past the threshold the row turns red and the phone ticks once: letting go now deletes.
    val armed = width > 0 && -offset >= width * DELETE_THRESHOLD
    LaunchedEffect(armed) {
        if (armed) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
    Box(modifier.onSizeChanged { width = it.width }) {
        if (offset < 0f) {
            val background by animateColorAsState(
                if (armed) colors.danger else colors.danger.copy(alpha = 0.18f),
                label = "deleteBackground",
            )
            val icon by animateColorAsState(if (armed) colors.onDanger else colors.danger, label = "deleteIcon")
            Row(
                Modifier.matchParentSize().background(background).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(TideIcons.Delete), contentDescription = null, tint = icon)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta -> offset = (offset + delta).coerceIn(-width.toFloat(), 0f) },
                    orientation = Orientation.Horizontal,
                    reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                    onDragStopped = {
                        if (-offset >= width * DELETE_THRESHOLD) {
                            animate(offset, -width.toFloat(), animationSpec = tween(DELETE_SLIDE_MILLIS)) { value, _ -> offset = value }
                            onDelete()
                        } else {
                            animate(offset, 0f, animationSpec = spring()) { value, _ -> offset = value }
                        }
                    },
                ),
        ) {
            content()
        }
    }
}

private const val DELETE_SLIDE_MILLIS = 180
