package com.iqbalwork.capsule.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ShortcutsState
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.ui.components.ExpandedChargingCard
import com.iqbalwork.capsule.ui.components.ExpandedMediaCard
import com.iqbalwork.capsule.ui.components.ExpandedNotificationCard
import com.iqbalwork.capsule.ui.components.ExpandedShortcutsCard
import com.iqbalwork.capsule.ui.components.ExpandedSliderCard
import com.iqbalwork.capsule.ui.components.ExpandedTimerCard
import com.iqbalwork.capsule.ui.components.MiniCapsuleView
import com.iqbalwork.capsule.ui.components.RedTargetCalibratorView
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

/**
 * The capsule itself: a black pill sized from the user's geometry that springs open into
 * the card carousel. Shared by the overlay window and the companion app preview.
 */
@Composable
fun CapsuleRootView(
    state: CapsuleState,
    dispatch: (Action) -> Unit,
    expandedWidth: Dp,
    expandedHeight: Dp,
    modifier: Modifier = Modifier
) {
    val settings = state.settings
    val geometry = settings.geometry(state.visibility.orientation)
    val mode = state.mode
    val expanded = mode == CapsuleMode.EXPANDED
    val haptics = LocalHapticFeedback.current
    val sizeSpring = if (expanded) CapsuleTokens.FruitSpringDp else CapsuleTokens.CollapseSpringDp

    val width by animateDpAsState(
        targetValue = if (expanded) expandedWidth
        else OverlayPlacement.pillWidthDp(geometry, miniEvent = mode == CapsuleMode.MINI_EVENT).dp,
        animationSpec = sizeSpring,
        label = "capsule_width"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) expandedHeight else geometry.heightDp.dp,
        animationSpec = sizeSpring,
        label = "capsule_height"
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) CapsuleTokens.ExpandedCornerRadius else geometry.cornerDp.dp,
        animationSpec = sizeSpring,
        label = "capsule_corner"
    )
    val alpha = if (mode == CapsuleMode.IDLE) settings.idleOpacity else 1f
    val shape = RoundedCornerShape(corner)

    fun gesture(type: GestureType, haptic: HapticFeedbackType = HapticFeedbackType.TextHandleMove) {
        if (settings.hapticsEnabled && settings.gestureAction(type) != GestureAction.NONE) {
            haptics.performHapticFeedback(haptic)
        }
        dispatch(UserIntent.Gesture(type))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(width = width, height = height)
                .graphicsLayer { this.alpha = alpha }
                .notificationGlow(
                    color = state.topNotification?.glowColor,
                    enabled = settings.notificationGlow && mode == CapsuleMode.COMPACT &&
                        state.activeCard == CapsuleCardType.NOTIFICATION,
                    shape = shape
                )
                .clip(shape)
                .capsuleGestures(
                    tapsEnabled = !expanded,
                    doubleTapEnabled = settings.gestureAction(GestureType.DOUBLE_TAP) != GestureAction.NONE,
                    onTap = { gesture(GestureType.TAP) },
                    onDoubleTap = { gesture(GestureType.DOUBLE_TAP) },
                    onLongPress = { gesture(GestureType.LONG_PRESS, HapticFeedbackType.LongPress) },
                    onSwipeLeft = { gesture(GestureType.SWIPE_LEFT) },
                    onSwipeRight = { gesture(GestureType.SWIPE_RIGHT) },
                    onSwipeUp = { dispatch(UserIntent.SwipeUp) }
                ),
            shape = shape,
            color = CapsuleTokens.Shell,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            // Keyed on expanded/collapsed only: the outgoing side keeps rendering the state it
            // last had (e.g. a dismissed notification) and fades out while the shell shrinks,
            // instead of the content vanishing and an empty black box shrinking.
            AnimatedContent(
                targetState = ShellFrame(expanded, state),
                // A mini event appearing or ending also crossfades inside the pill.
                contentKey = { if (it.expanded) "card" else "pill:${it.state.miniEvent?.id}" },
                transitionSpec = {
                    fadeIn(tween(durationMillis = 180, delayMillis = 90)) togetherWith fadeOut(tween(durationMillis = 110))
                },
                label = "shell_content"
            ) { frame ->
                if (frame.expanded) {
                    // Laid out at full card size and clipped by the shell, so text does not
                    // reflow while the shell springs open or shut.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.TopCenter, unbounded = true)
                    ) {
                        ExpandedCarousel(frame.state, dispatch, Modifier.size(expandedWidth, expandedHeight))
                    }
                } else {
                    MiniCapsuleView(state = frame.state)
                }
            }
        }

        // Drawn on top of the pill: when aligned, the camera hole hides it completely.
        if (settings.showCalibrationTarget && !expanded) {
            RedTargetCalibratorView(targetSizeDp = minOf(geometry.heightDp, 40))
        }
    }
}

/** F-20: a pulsing outline in the app's color while a notification waits in the pill. */
@Composable
private fun Modifier.notificationGlow(color: Long?, enabled: Boolean, shape: Shape): Modifier {
    if (!enabled || color == null) return this
    val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    return border(2.dp, Color(color).copy(alpha = pulse), shape)
}

private data class ShellFrame(val expanded: Boolean, val state: CapsuleState)

/** One carousel page; each notification is its own page so the next one slides in on dismiss. */
private data class CardFrame(val card: CapsuleCardType, val notificationKey: String?, val state: CapsuleState)

@Composable
private fun ExpandedCarousel(state: CapsuleState, dispatch: (Action) -> Unit, modifier: Modifier = Modifier) {
    val cards = state.activeCards
    val active = state.activeCard ?: return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = CardFrame(
                    card = active,
                    notificationKey = state.topNotification?.key.takeIf { active == CapsuleCardType.NOTIFICATION },
                    state = state
                ),
                contentKey = { it.card to it.notificationKey },
                transitionSpec = {
                    val from = cards.indexOf(initialState.card)
                    val to = cards.indexOf(targetState.card)
                    val direction = if (from == -1 || to >= from) 1 else -1
                    (slideInHorizontally(CapsuleTokens.FruitSpringIntOffset) { direction * it } +
                        fadeIn(CapsuleTokens.FruitSpring)) togetherWith
                        (slideOutHorizontally(CapsuleTokens.FruitSpringIntOffset) { -direction * it } +
                            fadeOut(CapsuleTokens.FruitSpring))
                },
                label = "carousel_card"
            ) { frame ->
                CardContent(frame.card, frame.state, dispatch)
            }
        }

        if (cards.size > 1) {
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cards.forEach { card ->
                    val selected = card == active
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CardContent(card: CapsuleCardType, state: CapsuleState, dispatch: (Action) -> Unit) {
    when (card) {
        CapsuleCardType.MEDIA -> state.media?.let { ExpandedMediaCard(mediaState = it, onAction = dispatch) }
        CapsuleCardType.SLIDERS -> ExpandedSliderCard(sliderState = state.sliders, onAction = dispatch)
        CapsuleCardType.CHARGING -> ExpandedChargingCard(
            chargingState = state.charging,
            fahrenheit = state.settings.temperatureFahrenheit
        )
        CapsuleCardType.NOTIFICATION -> state.topNotification?.let { notification ->
            ExpandedNotificationCard(
                notification = notification,
                showText = state.showsText(notification),
                replying = state.replyingTo == notification.key,
                queuedCount = state.notifications.size,
                onAction = dispatch
            )
        }
        CapsuleCardType.TIMER -> state.timer?.let { ExpandedTimerCard(timerState = it, onAction = dispatch) }
        // Static shortcut cards arrive in M6.
        CapsuleCardType.SHORTCUTS -> ExpandedShortcutsCard(shortcutsState = ShortcutsState(), onAction = dispatch)
    }
}
