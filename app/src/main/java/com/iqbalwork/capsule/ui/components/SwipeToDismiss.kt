package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** How far a card must be dragged sideways, in dp, before letting go dismisses it. */
const val DISMISS_DRAG_DP = 96

/** Furthest the card fades while being dragged; it never disappears before it is dismissed. */
private const val MAX_FADE = 0.6f

/** Whether letting go after dragging [dragPx] (either direction) dismisses. */
fun shouldDismiss(dragPx: Float, thresholdPx: Float): Boolean = abs(dragPx) >= thresholdPx

/** 0 (untouched) to [MAX_FADE] as the drag nears twice the threshold; used to fade the card out. */
fun dragFade(dragPx: Float, thresholdPx: Float): Float =
    (abs(dragPx) / (2f * thresholdPx)).coerceIn(0f, 1f) * MAX_FADE

/**
 * Drag sideways to dismiss (F-23). The card follows the finger and fades; past the threshold, letting
 * go calls [onDismiss], otherwise it springs back. The horizontal drag is consumed, so the capsule's
 * own swipe (next/previous card) does not also fire. Vertical movement is left alone: swiping up
 * still collapses the capsule. Keyed on [key] so a new notification starts from rest.
 */
@Composable
fun Modifier.swipeToDismiss(enabled: Boolean, key: Any, onDismiss: () -> Unit): Modifier {
    if (!enabled) return this
    var dragX by remember(key) { mutableFloatStateOf(0f) }
    val thresholdPx = with(LocalDensity.current) { DISMISS_DRAG_DP.dp.toPx() }
    return this
        .graphicsLayer {
            translationX = dragX
            alpha = 1f - dragFade(dragX, thresholdPx)
        }
        .pointerInput(key) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    dragX += delta
                },
                onDragEnd = {
                    if (shouldDismiss(dragX, thresholdPx)) onDismiss() else dragX = 0f
                },
                onDragCancel = { dragX = 0f }
            )
        }
}
