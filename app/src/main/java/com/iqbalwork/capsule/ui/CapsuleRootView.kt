package com.iqbalwork.capsule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.engine.state.CapsuleStateEngine
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.CutoutAlignment
import com.iqbalwork.capsule.ui.components.ExpandedChargingCard
import com.iqbalwork.capsule.ui.components.ExpandedMediaCard
import com.iqbalwork.capsule.ui.components.ExpandedNotificationCard
import com.iqbalwork.capsule.ui.components.ExpandedShortcutsCard
import com.iqbalwork.capsule.ui.components.ExpandedSliderCard
import com.iqbalwork.capsule.ui.components.ExpandedTimerCard
import com.iqbalwork.capsule.ui.components.MiniCapsuleView
import com.iqbalwork.capsule.ui.components.RedTargetCalibratorView
import com.iqbalwork.capsule.ui.theme.CapsuleTokens
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Root interactive view for the Dynamic Capsule Island.
 * Supports complete 5-Way Gestures:
 * 1. Single Tap: Toggle Expand / Collapse
 * 2. Double Tap: Quick action (Media Play/Pause or Torch/Mute toggle)
 * 3. Long Press: Full expansion with haptic feedback
 * 4. Swipe Left: Carousel nextCard
 * 5. Swipe Right: Carousel previousCard
 */
@Composable
fun CapsuleRootView(
    engine: CapsuleStateEngine = CapsuleStateEngine.instance,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Spring Animations for Capsule Container Dimensions
    val animatedWidth by animateDpAsState(
        targetValue = if (state.isExpanded) CapsuleTokens.ExpandedWidth else state.cutoutPreferences.capsuleWidthDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "capsule_width"
    )

    val animatedHeight by animateDpAsState(
        targetValue = if (state.isExpanded) CapsuleTokens.ExpandedHeight else state.cutoutPreferences.capsuleHeightDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "capsule_height"
    )

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (state.isExpanded) CapsuleTokens.ExpandedCornerRadius else state.cutoutPreferences.cornerRadiusDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "capsule_corner_radius"
    )

    val currentOpacity = if (state.isExpanded) 1.0f else state.cutoutPreferences.idleOpacity

    // Cutout Alignment Offset
    val alignmentOffsetX = when (state.cutoutPreferences.alignment) {
        CutoutAlignment.CENTER -> 0.dp
        CutoutAlignment.LEFT -> (-100).dp
        CutoutAlignment.RIGHT -> 100.dp
        CutoutAlignment.CUSTOM -> state.cutoutPreferences.offsetX.dp
    }
    val alignmentOffsetY = state.cutoutPreferences.offsetY.dp

    Box(
        modifier = modifier
            .offset(x = alignmentOffsetX, y = alignmentOffsetY),
        contentAlignment = Alignment.TopCenter
    ) {
        // Red Target Calibrator for physical alignment
        if (state.cutoutPreferences.showRedTarget) {
            RedTargetCalibratorView(
                targetSizeDp = 40,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Main Capsule Shell
        Surface(
            modifier = Modifier
                .size(width = animatedWidth, height = animatedHeight)
                .alpha(currentOpacity)
                .shadow(
                    elevation = if (state.isExpanded) 16.dp else 4.dp,
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .clip(RoundedCornerShape(animatedCornerRadius))
                .border(
                    width = 1.dp,
                    color = CapsuleTokens.OutlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(animatedCornerRadius)
                )
                .capsule5WayGestures(
                    haptics = haptics,
                    coroutineScope = coroutineScope,
                    onTap = {
                        engine.dispatch(UserAction.Tap)
                    },
                    onDoubleTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        engine.dispatch(UserAction.DoubleTap)
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        engine.dispatch(UserAction.LongPress)
                    },
                    onSwipeLeft = {
                        engine.dispatch(UserAction.SwipeLeft)
                    },
                    onSwipeRight = {
                        engine.dispatch(UserAction.SwipeRight)
                    }
                ),
            color = CapsuleTokens.Surface,
            contentColor = CapsuleTokens.OnSurface
        ) {
            if (!state.isExpanded) {
                // Collapsed Mini Capsule View
                MiniCapsuleView(state = state)
            } else {
                // Expanded Carousel Card Container
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Carousel Card Transition with FruitSpring
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = state.activeCard,
                            transitionSpec = {
                                val forward = targetState.ordinal >= initialState.ordinal
                                if (forward) {
                                    (slideInHorizontally(
                                        animationSpec = CapsuleTokens.FruitSpringIntOffset,
                                        initialOffsetX = { it }
                                    ) + fadeIn(animationSpec = CapsuleTokens.FruitSpring)) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = CapsuleTokens.FruitSpringIntOffset,
                                                targetOffsetX = { -it }
                                            ) + fadeOut(animationSpec = CapsuleTokens.FruitSpring))
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = CapsuleTokens.FruitSpringIntOffset,
                                        initialOffsetX = { -it }
                                    ) + fadeIn(animationSpec = CapsuleTokens.FruitSpring)) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = CapsuleTokens.FruitSpringIntOffset,
                                                targetOffsetX = { it }
                                            ) + fadeOut(animationSpec = CapsuleTokens.FruitSpring))
                                }
                            },
                            label = "carousel_card"
                        ) { activeCard ->
                            when (activeCard) {
                                CapsuleCardType.MEDIA -> ExpandedMediaCard(
                                    mediaState = state.mediaState,
                                    onAction = { engine.dispatch(it) }
                                )
                                CapsuleCardType.SLIDERS -> ExpandedSliderCard(
                                    sliderState = state.sliderState,
                                    onAction = { engine.dispatch(it) }
                                )
                                CapsuleCardType.CHARGING -> ExpandedChargingCard(
                                    chargingState = state.chargingState
                                )
                                CapsuleCardType.NOTIFICATION -> ExpandedNotificationCard(
                                    notificationState = state.notificationState,
                                    onAction = { engine.dispatch(it) }
                                )
                                CapsuleCardType.TIMER -> ExpandedTimerCard(
                                    timerState = state.timerState,
                                    onAction = { engine.dispatch(it) }
                                )
                                CapsuleCardType.SHORTCUTS -> ExpandedShortcutsCard(
                                    shortcutsState = state.shortcutsState,
                                    onAction = { engine.dispatch(it) }
                                )
                            }
                        }
                    }

                    // Carousel Indicators
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.availableCards.forEachIndexed { index, cardType ->
                            val isSelected = cardType == state.activeCard
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) CapsuleTokens.Primary else CapsuleTokens.OutlineVariant
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom 5-Way Gesture recognizer handling:
 * - Single Tap (toggle expand/collapse)
 * - Double Tap (shortcut action)
 * - Long Press (expand full card with haptics)
 * - Horizontal Drag Left (Swipe Left -> next card)
 * - Horizontal Drag Right (Swipe Right -> previous card)
 */
fun Modifier.capsule5WayGestures(
    haptics: HapticFeedback,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier = pointerInput(Unit) {
    val doubleTapTimeoutMillis = 300L
    val longPressTimeoutMillis = 450L
    val swipeSlop = 40f

    var lastTapTime = 0L
    var pendingSingleTapJob: Job? = null

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startX = down.position.x
        val startY = down.position.y
        var isLongPressTriggered = false
        var isSwipeTriggered = false

        val longPressJob = coroutineScope.launch {
            delay(longPressTimeoutMillis)
            isLongPressTriggered = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress()
        }

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break

            if (!change.pressed) {
                // Finger lifted
                longPressJob.cancel()
                if (!isLongPressTriggered && !isSwipeTriggered) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < doubleTapTimeoutMillis) {
                        pendingSingleTapJob?.cancel()
                        pendingSingleTapJob = null
                        lastTapTime = 0L
                        onDoubleTap()
                    } else {
                        lastTapTime = currentTime
                        pendingSingleTapJob = coroutineScope.launch {
                            delay(doubleTapTimeoutMillis)
                            onTap()
                        }
                    }
                }
                break
            }

            val dx = change.position.x - startX
            val dy = change.position.y - startY

            // Check if user is swiping horizontally
            if (abs(dx) > swipeSlop * 1.5f || abs(dy) > swipeSlop * 1.5f) {
                longPressJob.cancel()
                if (!isSwipeTriggered && abs(dx) > abs(dy) * 1.2f && abs(dx) > swipeSlop * 2f) {
                    isSwipeTriggered = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (dx < 0) {
                        onSwipeLeft()
                    } else {
                        onSwipeRight()
                    }
                }
            }
        }
    }
}
