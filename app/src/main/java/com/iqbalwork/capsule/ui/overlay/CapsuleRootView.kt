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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.iqbalwork.capsule.core.settings.StaticCardType
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CardPage
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.ui.components.ExpandedChargingCard
import com.iqbalwork.capsule.ui.components.ExpandedMediaCard
import com.iqbalwork.capsule.ui.components.ExpandedNotificationCard
import com.iqbalwork.capsule.ui.components.ExpandedProgressCard
import com.iqbalwork.capsule.ui.components.ExpandedShortcutsCard
import com.iqbalwork.capsule.ui.components.ExpandedSliderCard
import com.iqbalwork.capsule.ui.components.ExpandedTimerCard
import com.iqbalwork.capsule.ui.components.ExpandedBarcodeCard
import com.iqbalwork.capsule.ui.components.ExpandedIntentCard
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
        if (settings.hapticsEnabled && state.gesture(type).action != GestureAction.NONE) {
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
                    doubleTapEnabled = state.gesture(GestureType.DOUBLE_TAP).action != GestureAction.NONE,
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
                        val cardSize = Modifier.size(expandedWidth, expandedHeight)
                        if (frame.state.showsStaticList) {
                            ExpandedStaticList(frame.state, dispatch, cardSize)
                        } else {
                            ExpandedCarousel(frame.state, dispatch, cardSize)
                        }
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

/** One carousel page keyed by page key + notification key (for per-notification animation). */
private data class PageFrame(val pageKey: String, val notificationKey: String?, val state: CapsuleState)

@Composable
private fun ExpandedCarousel(state: CapsuleState, dispatch: (Action) -> Unit, modifier: Modifier = Modifier) {
    val pages = state.pages
    val active = state.activePage ?: return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = PageFrame(
                    pageKey = active.key,
                    notificationKey = state.topNotification?.key.takeIf {
                        active is CardPage.Dynamic && active.type == CapsuleCardType.NOTIFICATION
                    },
                    state = state
                ),
                contentKey = { it.pageKey to it.notificationKey },
                transitionSpec = {
                    val from = pages.indexOfFirst { it.key == initialState.pageKey }
                    val to = pages.indexOfFirst { it.key == targetState.pageKey }
                    val direction = if (from == -1 || to >= from) 1 else -1
                    (slideInHorizontally(CapsuleTokens.FruitSpringIntOffset) { direction * it } +
                        fadeIn(CapsuleTokens.FruitSpring)) togetherWith
                        (slideOutHorizontally(CapsuleTokens.FruitSpringIntOffset) { -direction * it } +
                            fadeOut(CapsuleTokens.FruitSpring))
                },
                label = "carousel_card"
            ) { frame ->
                PageContent(active, frame.state, dispatch)
            }
        }

        if (pages.size > 1) {
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEach { page ->
                    val selected = page.key == active.key
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

/**
 * LIST view (spec line 81): the same static cards stacked vertically instead of side by side.
 * Every row reuses [StaticCardContent], so a card cannot look different between the two modes;
 * the panel scrolls when there are more rows than [OverlayPlacement.MAX_LIST_ROWS].
 */
@Composable
private fun ExpandedStaticList(state: CapsuleState, dispatch: (Action) -> Unit, modifier: Modifier = Modifier) {
    val pages = state.pages.filterIsInstance<CardPage.Static>()
    val active = state.activePage
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        pages.forEach { page ->
            val selected = page.key == active?.key
            val rowShape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, rowShape)
                        } else {
                            Modifier
                        }
                    )
            ) {
                StaticCardContent(page, state, dispatch)
            }
        }
    }
}

@Composable
private fun PageContent(page: CardPage, state: CapsuleState, dispatch: (Action) -> Unit) {
    when (page) {
        is CardPage.Dynamic -> DynamicCardContent(page.type, state, dispatch)
        is CardPage.Static -> StaticCardContent(page, state, dispatch)
    }
}

@Composable
private fun DynamicCardContent(card: CapsuleCardType, state: CapsuleState, dispatch: (Action) -> Unit) {
    when (card) {
        CapsuleCardType.MEDIA -> state.media?.let { ExpandedMediaCard(mediaState = it, onAction = dispatch) }
        CapsuleCardType.SLIDERS -> ExpandedSliderCard(
            sliders = state.sliders,
            style = state.settings.sliderStyle,
            onAction = dispatch
        )
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
        CapsuleCardType.TIMER -> state.timer?.let { ExpandedTimerCard(timer = it, onAction = dispatch) }
        CapsuleCardType.PROGRESS -> ExpandedProgressCard(items = state.progress)
        CapsuleCardType.SHORTCUTS -> ExpandedShortcutsCard(
            shortcuts = state.settings.shortcuts,
            themedIcons = state.settings.themedShortcutIcons,
            onAction = dispatch,
            barcode = state.settings.barcode,
            barcodeEnabled = state.settings.barcodeCardEnabled
        )
    }
}

@Composable
private fun StaticCardContent(page: CardPage.Static, state: CapsuleState, dispatch: (Action) -> Unit) {
    when (page.card.type) {
        StaticCardType.SHORTCUTS -> ExpandedShortcutsCard(
            shortcuts = state.settings.shortcuts,
            themedIcons = state.settings.themedShortcutIcons,
            onAction = dispatch,
            barcode = state.settings.barcode,
            barcodeEnabled = state.settings.barcodeCardEnabled
        )
        StaticCardType.BARCODE -> ExpandedBarcodeCard(
            barcode = state.settings.barcode
        )
        StaticCardType.SLIDERS -> ExpandedSliderCard(
            sliders = state.sliders,
            style = state.settings.sliderStyle,
            onAction = dispatch
        )
        StaticCardType.INTENT -> ExpandedIntentCard(
            intents = state.settings.customIntents,
            onRun = { dispatch(UserIntent.RunCustomIntent(it)) }
        )
    }
}
