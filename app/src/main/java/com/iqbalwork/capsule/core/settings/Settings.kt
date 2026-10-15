package com.iqbalwork.capsule.core.settings

/**
 * Capsule geometry for one orientation. Offsets are relative to the default position,
 * which is centered on the camera cutout (or top-center of the screen when there is none).
 */
data class Geometry(
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 0,
    val widthDp: Int = 120,
    val heightDp: Int = 36,
    val cornerDp: Int = 18
)

enum class Orientation { PORTRAIT, LANDSCAPE }

enum class ColorSource { DYNAMIC, BRAND }

/** How long a paused player stays in the capsule (F-12). */
enum class MediaCollapseDelay(val millis: Long?) {
    INSTANT(0L),
    SECONDS_5(5_000L),
    SECONDS_15(15_000L),
    SECONDS_30(30_000L),
    SECONDS_60(60_000L),
    MINUTES_2(120_000L),
    NEVER(null)
}

enum class GestureType { TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT }

enum class GestureAction {
    NONE,
    TOGGLE_EXPAND,
    NEXT_CARD,
    PREVIOUS_CARD,
    MEDIA_PLAY_PAUSE,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS
}

val DefaultGestures: Map<GestureType, GestureAction> = mapOf(
    GestureType.TAP to GestureAction.TOGGLE_EXPAND,
    GestureType.DOUBLE_TAP to GestureAction.MEDIA_PLAY_PAUSE,
    GestureType.LONG_PRESS to GestureAction.OPEN_NOTIFICATIONS,
    GestureType.SWIPE_LEFT to GestureAction.NEXT_CARD,
    GestureType.SWIPE_RIGHT to GestureAction.PREVIOUS_CARD
)

data class Settings(
    val portrait: Geometry = Geometry(),
    val landscape: Geometry = Geometry(),
    val idleOpacity: Float = 1f,
    val alwaysShowPill: Boolean = true,
    val showCalibrationTarget: Boolean = false,
    val colorSource: ColorSource = ColorSource.DYNAMIC,
    val hapticsEnabled: Boolean = true,
    val hideInFullscreen: Boolean = true,
    val hideWithKeyboard: Boolean = true,
    val hideInLandscape: Boolean = true,
    val gestures: Map<GestureType, GestureAction> = DefaultGestures,
    val mediaEnabled: Boolean = true,
    val mediaCollapseDelay: MediaCollapseDelay = MediaCollapseDelay.SECONDS_30,
    val hideMediaWhenAppInFront: Boolean = true,
    val disclosureAccepted: Boolean = false
) {
    fun geometry(orientation: Orientation): Geometry =
        if (orientation == Orientation.LANDSCAPE) landscape else portrait

    fun withGeometry(orientation: Orientation, geometry: Geometry): Settings =
        if (orientation == Orientation.LANDSCAPE) copy(landscape = geometry) else copy(portrait = geometry)

    fun gestureAction(type: GestureType): GestureAction =
        gestures[type] ?: DefaultGestures.getValue(type)
}
