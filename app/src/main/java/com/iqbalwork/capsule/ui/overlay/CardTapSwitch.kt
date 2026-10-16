package com.iqbalwork.capsule.ui.overlay

import com.iqbalwork.capsule.core.settings.GestureAction

/**
 * Which way a tap on the dots strip goes: the left half is the previous card, the right half the
 * next. Physical halves, like the swipe gestures, so it does not flip with the layout direction.
 */
fun tapSwitchAction(x: Float, width: Float): GestureAction =
    if (x < width / 2f) GestureAction.PREVIOUS_CARD else GestureAction.NEXT_CARD

/** Height of the dots strip: tall enough to tap, but only when tapping is on, so the card keeps its room. */
const val TAP_STRIP_MIN_HEIGHT_DP = 22
