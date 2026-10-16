package com.iqbalwork.capsule.ui.overlay

import androidx.compose.animation.AnimatedContent
import com.iqbalwork.capsule.platform.accessibility.MiniEventWidth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectTapGestures
import com.iqbalwork.capsule.ui.components.progressStyle
import com.iqbalwork.capsule.ui.theme.resolveAppearance
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.iqbalwork.capsule.ui.theme.CapsuleMotion
import com.iqbalwork.capsule.ui.theme.CapsuleTokens
import com.iqbalwork.capsule.ui.theme.capsuleShape
import com.iqbalwork.capsule.ui.theme.collapsedCornerDp

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
    modifier: Modifier = Modifier,
    /** Space kept free at the top of an opened card, below the status bar (camera mode). */
    contentTopInset: Dp = 0.dp,
    /** Strip over the camera hole that pill content must stay out of (see WindowPlacement.cameraGapDp). */
    cameraGapDp: Int = 0,
    /** Height added under the pill while a pop-up event is shown, for its row under the camera hole. */
    eventRowDp: Int = 0
) {
    val settings = state.settings
    val geometry = state.geometry
    val mode = state.mode
    val expanded = mode == CapsuleMode.EXPANDED
    val haptics = LocalHapticFeedback.current
    val motion = remember(settings.animationSpeed, settings.bounce, settings.expandStyle, settings.reduceAnimations) {
        CapsuleMotion.of(settings)
    }
    // The same estimate the window was sized with, so the pill and its window agree on the width.
    val miniContent = remember(state.miniEvent, settings.eventStyles) {
        state.miniEvent?.let { MiniEventWidth.of(it, settings.eventStyle(it.type.category)) }
    }
    val sizeSpring = if (expanded) motion.expand.spec<Dp>() else motion.collapse.spec<Dp>()

    val width by animateDpAsState(
        targetValue = if (expanded) expandedWidth
        else OverlayPlacement.pillWidthDp(geometry, miniEvent = mode == CapsuleMode.MINI_EVENT, cameraGapDp = cameraGapDp, content = miniContent).dp,
        animationSpec = sizeSpring,
        label = "capsule_width"
    )
    val height by animateDpAsState(
        targetValue = when {
            expanded -> expandedHeight
            mode == CapsuleMode.MINI_EVENT -> (geometry.heightDp + eventRowDp).dp
            else -> geometry.heightDp.dp
        },
        animationSpec = sizeSpring,
        label = "capsule_height"
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) settings.cardCornerDp.dp
        else collapsedCornerDp(settings.shapeStyle, geometry.cornerDp, geometry.heightDp),
        animationSpec = sizeSpring,
        label = "capsule_corner"
    )
    val alpha = if (mode == CapsuleMode.IDLE) settings.idleOpacity else 1f
    val shape = capsuleShape(settings.shapeStyle, corner)
    val night = isSystemInDarkTheme()
    val appearance = remember(settings, night) { resolveAppearance(settings, night) }
    val shellColor = Color(appearance.shellArgb)
    // Black where the camera is, fading into the background color: the pill blends with the hole.
    val shellBrush = if (appearance.cutoutGradient) {
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = shellColor.alpha), shellColor))
    } else null
    val borderColor = settings.borderColor?.let { Color(it) } ?: MaterialTheme.colorScheme.outline

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
                // Shadow and border sit outside the clip below so the clip cannot cut them off.
                .then(if (appearance.shadowDp > 0) Modifier.shadow(appearance.shadowDp.dp, shape, clip = false) else Modifier)
                .then(if (appearance.borderDp > 0) Modifier.border(appearance.borderDp.dp, borderColor, shape) else Modifier)
                .notificationGlow(
                    color = state.topNotification?.glowColor,
                    enabled = settings.notificationGlow && mode == CapsuleMode.COMPACT &&
                        state.activeCard == CapsuleCardType.NOTIFICATION,
                    shape = shape
                )
                .clip(shape)
                .then(if (shellBrush != null) Modifier.background(shellBrush) else Modifier)
                .capsuleSemantics(state, expanded, dispatch)
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
            color = if (shellBrush != null) Color.Transparent else shellColor,
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
                            ExpandedStaticList(frame.state, dispatch, cardSize.padding(top = contentTopInset))
                        } else {
                            ExpandedCarousel(frame.state, dispatch, motion, cardSize.padding(top = contentTopInset))
                        }
                    }
                } else {
                    MiniCapsuleView(
                        state = frame.state,
                        cameraGapDp = cameraGapDp,
                        belowCameraTopDp = if (eventRowDp > 0) geometry.heightDp else 0
                    )
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
private fun ExpandedCarousel(
    state: CapsuleState,
    dispatch: (Action) -> Unit,
    motion: CapsuleMotion,
    modifier: Modifier = Modifier
) {
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
                    if (!motion.slidesCards) {
                        fadeIn(motion.content.spec()) togetherWith fadeOut(motion.content.spec())
                    } else {
                        val from = pages.indexOfFirst { it.key == initialState.pageKey }
                        val to = pages.indexOfFirst { it.key == targetState.pageKey }
                        val direction = if (from == -1 || to >= from) 1 else -1
                        (slideInHorizontally(motion.content.spec()) { direction * it } +
                            fadeIn(motion.content.spec())) togetherWith
                            (slideOutHorizontally(motion.content.spec()) { -direction * it } +
                                fadeOut(motion.content.spec()))
                    }
                },
                label = "carousel_card"
            ) { frame ->
                PageContent(active, frame.state, dispatch)
            }
        }

        if (pages.size > 1) {
            // Full width, so a swipe anywhere along this strip switches cards even where the card
            // above consumes sideways drags (swipe to dismiss).
            val tapSwitch = state.settings.cardTapSwitch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (tapSwitch) {
                            Modifier
                                .heightIn(min = TAP_STRIP_MIN_HEIGHT_DP.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        dispatch(UserIntent.Perform(tapSwitchAction(offset.x, size.width.toFloat())))
                                    }
                                }
                        } else {
                            Modifier.padding(bottom = 6.dp)
                        }
                    ),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
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
                onAction = dispatch,
                swipeToDismiss = state.settings.notificationSwipeDismiss
            )
        }
        CapsuleCardType.TIMER -> state.timer?.let { ExpandedTimerCard(timer = it, onAction = dispatch) }
        CapsuleCardType.PROGRESS -> ExpandedProgressCard(items = state.progress, style = state.settings.progressStyle())
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
