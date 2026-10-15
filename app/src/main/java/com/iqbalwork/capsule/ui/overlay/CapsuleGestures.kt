package com.iqbalwork.capsule.ui.overlay

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Tap, double tap, long press, horizontal swipes and swipe up on the capsule (spec §2.5.9).
 *
 * A drag that leaves the touch slop never counts as a tap, even when it is too short to be
 * a swipe. When [doubleTapEnabled] is false a single tap fires immediately instead of
 * waiting out the double-tap window. With [tapsEnabled] false (an expanded card, whose own
 * buttons handle taps) only swipes are detected. Pointer changes a child already consumed,
 * like a slider drag, are ignored.
 */
fun Modifier.capsuleGestures(
    tapsEnabled: Boolean,
    doubleTapEnabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeUp: () -> Unit
): Modifier = pointerInput(tapsEnabled, doubleTapEnabled) {
    val swipeThreshold = 48.dp.toPx()
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
    var pendingTap: Job? = null

    coroutineScope {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var moved = false
            var handled = false
            val longPress = launch {
                if (!tapsEnabled) return@launch
                delay(longPressTimeout)
                handled = true
                onLongPress()
            }

            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) {
                    handled = true
                    longPress.cancel()
                }
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    moved = true
                    longPress.cancel()
                }
                if (moved && !handled) {
                    if (abs(dx) >= swipeThreshold && abs(dx) > abs(dy) * 1.2f) {
                        handled = true
                        if (dx < 0) onSwipeLeft() else onSwipeRight()
                    } else if (-dy >= swipeThreshold && abs(dy) > abs(dx) * 1.2f) {
                        handled = true
                        onSwipeUp()
                    }
                }
                if (!change.pressed) break
            }
            longPress.cancel()
            if (moved || handled || !tapsEnabled) return@awaitEachGesture

            when {
                !doubleTapEnabled -> onTap()
                pendingTap?.isActive == true -> {
                    pendingTap?.cancel()
                    pendingTap = null
                    onDoubleTap()
                }
                else -> pendingTap = launch {
                    delay(doubleTapTimeout)
                    onTap()
                }
            }
        }
    }
}
