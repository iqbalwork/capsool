package com.iqbalwork.capsule.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val width by animateDpAsState(
        targetValue = if (expanded) expandedWidth else geometry.widthDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "capsule_width"
    )
    val height by animateDpAsState(
        targetValue = if (expanded) expandedHeight else geometry.heightDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "capsule_height"
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) CapsuleTokens.ExpandedCornerRadius else geometry.cornerDp.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
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
            if (expanded) ExpandedCarousel(state, dispatch) else MiniCapsuleView(state = state)
        }

        // Drawn on top of the pill: when aligned, the camera hole hides it completely.
        if (settings.showCalibrationTarget && !expanded) {
            RedTargetCalibratorView(targetSizeDp = minOf(geometry.heightDp, 40))
        }
    }
}

@Composable
private fun ExpandedCarousel(state: CapsuleState, dispatch: (Action) -> Unit) {
    val cards = state.activeCards
    val active = state.activeCard ?: return
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = active,
                transitionSpec = {
                    val direction = if (cards.indexOf(targetState) >= cards.indexOf(initialState)) 1 else -1
                    (slideInHorizontally(CapsuleTokens.FruitSpringIntOffset) { direction * it } +
                        fadeIn(CapsuleTokens.FruitSpring)) togetherWith
                        (slideOutHorizontally(CapsuleTokens.FruitSpringIntOffset) { -direction * it } +
                            fadeOut(CapsuleTokens.FruitSpring))
                },
                label = "carousel_card"
            ) { card ->
                CardContent(card, state, dispatch)
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
        CapsuleCardType.CHARGING -> ExpandedChargingCard(chargingState = state.charging)
        CapsuleCardType.NOTIFICATION -> ExpandedNotificationCard(notificationState = state.notification, onAction = dispatch)
        CapsuleCardType.TIMER -> state.timer?.let { ExpandedTimerCard(timerState = it, onAction = dispatch) }
        // Static shortcut cards arrive in M6.
        CapsuleCardType.SHORTCUTS -> ExpandedShortcutsCard(shortcutsState = ShortcutsState(), onAction = dispatch)
    }
}
