package com.iqbalwork.capsule.core.settings

import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.model.EventCategory

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

/** F-24: start as a compact pill that expands on tap, or open the card right away. */
enum class NotificationDisplay { COMPACT, EXPANDED }

/** F-24: how long a notification stays in the capsule; null = until the user opens it. */
enum class NotificationDelay(val millis: Long?) {
    SECONDS_1(1_000L),
    SECONDS_3(3_000L),
    SECONDS_5(5_000L),
    SECONDS_10(10_000L),
    SECONDS_20(20_000L),
    SECONDS_30(30_000L),
    UNTIL_OPENED(null)
}

/** F-68: how long a mini event stays in the capsule. */
enum class MiniEventDuration(val millis: Long) {
    SECONDS_2(2_000L),
    SECONDS_3(3_000L),
    SECONDS_4(4_000L),
    SECONDS_6(6_000L)
}

/** Unlock greetings are opt-in; every other event starts enabled. */
val DefaultEventCategories: Set<EventCategory> = EventCategory.entries.toSet() - EventCategory.UNLOCK

/** F-51: visual style of the Controls card sliders. */
enum class SliderStyle { PILL, SPLIT, ROUNDED }

/** Brightness value most devices use for 100%; recalibrated per device (spec §2.5.5). */
const val DEFAULT_BRIGHTNESS_MAX = 255

enum class GestureType { TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT }

/** Everything a gesture can do (F-71, F-77). OPEN_APP needs a package as its target. */
enum class GestureAction {
    NONE,
    TOGGLE_EXPAND,
    NEXT_CARD,
    PREVIOUS_CARD,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREVIOUS,
    OPEN_CONTROLS,
    OPEN_SHORTCUTS,
    TOGGLE_FLASHLIGHT,
    OPEN_NOTIFICATIONS,
    OPEN_QUICK_SETTINGS,
    SCREENSHOT,
    LOCK_SCREEN,
    SCROLL_TO_TOP,
    OPEN_APP,
    CUSTOM_INTENT
}

/** A gesture's action plus its target, e.g. OPEN_APP and the package to open. */
data class GestureBinding(val action: GestureAction, val target: String? = null) {
    /** "ACTION" or "ACTION:target", as stored in preferences. */
    fun encode(): String = if (target == null) action.name else "${action.name}:$target"

    companion object {
        val None = GestureBinding(GestureAction.NONE)

        fun decode(value: String?): GestureBinding? {
            if (value == null) return null
            val action = runCatching { GestureAction.valueOf(value.substringBefore(':')) }.getOrNull() ?: return null
            return GestureBinding(action, value.substringAfter(':', "").ifEmpty { null })
        }
    }
}

/** F-75: what gestures do while something is showing in the capsule. */
val DefaultActiveGestures: Map<GestureType, GestureBinding> = mapOf(
    GestureType.TAP to GestureBinding(GestureAction.TOGGLE_EXPAND),
    GestureType.DOUBLE_TAP to GestureBinding(GestureAction.MEDIA_PLAY_PAUSE),
    GestureType.LONG_PRESS to GestureBinding(GestureAction.OPEN_CONTROLS),
    GestureType.SWIPE_LEFT to GestureBinding(GestureAction.NEXT_CARD),
    GestureType.SWIPE_RIGHT to GestureBinding(GestureAction.PREVIOUS_CARD)
)

/** F-75: what gestures do on the idle pill. Double tap is off so a tap reacts at once. */
val DefaultIdleGestures: Map<GestureType, GestureBinding> = mapOf(
    GestureType.TAP to GestureBinding(GestureAction.OPEN_CONTROLS),
    GestureType.DOUBLE_TAP to GestureBinding.None,
    GestureType.LONG_PRESS to GestureBinding(GestureAction.OPEN_SHORTCUTS),
    GestureType.SWIPE_LEFT to GestureBinding.None,
    GestureType.SWIPE_RIGHT to GestureBinding.None
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
    /** F-84: apps where the capsule stays hidden while they are in front. */
    val hiddenPackages: Set<String> = emptySet(),
    val gesturesActive: Map<GestureType, GestureBinding> = DefaultActiveGestures,
    val gesturesIdle: Map<GestureType, GestureBinding> = DefaultIdleGestures,
    /** F-73: send a broadcast for every gesture so Tasker/MacroDroid can react. */
    val automationBroadcast: Boolean = false,
    val shortcuts: List<Shortcut> = emptyList(),
    val themedShortcutIcons: Boolean = true,
    val mediaEnabled: Boolean = true,
    val mediaCollapseDelay: MediaCollapseDelay = MediaCollapseDelay.SECONDS_30,
    val hideMediaWhenAppInFront: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationDisplay: NotificationDisplay = NotificationDisplay.COMPACT,
    val notificationDelay: NotificationDelay = NotificationDelay.SECONDS_5,
    val notificationBlockedApps: Set<String> = emptySet(),
    /**
     * F-85. Source of truth for lock-screen behaviour. [hideNotificationTextWhenLocked] is kept in
     * sync with it while the old key is being retired, and new code must not read that field.
     */
    val lockScreenBehavior: LockScreenBehavior = LockScreenBehavior.SHOW_WITHOUT_CONTENT,
    /** Legacy mirror of [lockScreenBehavior]; read only by the settings migration. */
    val hideNotificationTextWhenLocked: Boolean = true,
    val removeNotificationAfterViewing: Boolean = false,
    val notificationGlow: Boolean = true,
    val chargingCardEnabled: Boolean = true,
    val enabledEvents: Set<EventCategory> = DefaultEventCategories,
    val miniEventDuration: MiniEventDuration = MiniEventDuration.SECONDS_3,
    val lowBatteryThreshold: Int = 15,
    val temperatureFahrenheit: Boolean = false,
    val sliderStyle: SliderStyle = SliderStyle.PILL,
    val progressEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val brightnessMax: Int = DEFAULT_BRIGHTNESS_MAX,
    val barcode: BarcodeContent = BarcodeContent(),
    val barcodeCardEnabled: Boolean = true,
    val customIntents: List<CustomIntent> = emptyList(),
    val staticCards: List<StaticCard> = emptyList(),
    val startCardId: String? = null,
    val staticCardViewMode: StaticCardViewMode = StaticCardViewMode.CAROUSEL,
    val disclosureAccepted: Boolean = false
) {
    /** Kartu statis yang benar-benar tampil. Daftar kosong = perilaku lama diturunkan otomatis. */
    val effectiveStaticCards: List<StaticCard>
        get() {
            if (staticCards.isNotEmpty()) return staticCards
            val derived = mutableListOf<StaticCard>()
            if (shortcuts.isNotEmpty()) {
                derived += StaticCard("default-shortcuts", StaticCardType.SHORTCUTS, StaticCardType.SHORTCUTS.label)
            }
            // Presence only: `core` must not reach into the barcode layer, and invalid content is
            // still shown so the card itself can explain what is wrong.
            if (barcodeCardEnabled && barcode.content.isNotBlank()) {
                derived += StaticCard("default-barcode", StaticCardType.BARCODE, StaticCardType.BARCODE.label)
            }
            if (customIntents.isNotEmpty()) {
                derived += StaticCard("default-intent", StaticCardType.INTENT, StaticCardType.INTENT.label)
            }
            return derived
        }

    /** `startCardId` yang menunjuk kartu tidak ada dianggap tidak diset. */
    val effectiveStartCardId: String?
        get() = if (effectiveStaticCards.any { it.id == startCardId }) startCardId else null

    fun geometry(orientation: Orientation): Geometry =
        if (orientation == Orientation.LANDSCAPE) landscape else portrait

    fun withGeometry(orientation: Orientation, geometry: Geometry): Settings =
        if (orientation == Orientation.LANDSCAPE) copy(landscape = geometry) else copy(portrait = geometry)

    fun gesture(type: GestureType, active: Boolean): GestureBinding =
        if (active) gesturesActive[type] ?: DefaultActiveGestures.getValue(type)
        else gesturesIdle[type] ?: DefaultIdleGestures.getValue(type)
}
