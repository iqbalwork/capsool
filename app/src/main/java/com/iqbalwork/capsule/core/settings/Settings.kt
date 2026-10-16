package com.iqbalwork.capsule.core.settings

import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.model.CapsuleCardType
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

/**
 * F-08: which calibration applies. The panel is identified by its short and long side in pixels, so
 * rotating the phone keeps the same panel while switching resolution (FHD+ to QHD+) or unfolding a
 * foldable gives another one, and each panel x orientation has its own geometry.
 */
data class ScreenProfileKey(val shortPx: Int, val longPx: Int, val orientation: Orientation) {
    /** "1080x2400:PORTRAIT", as stored in preferences. */
    fun encode(): String = "${shortPx}x$longPx:${orientation.name}"

    fun withOrientation(value: Orientation) = copy(orientation = value)

    companion object {
        /** From a window's pixel size in either rotation; null when the size is unknown. */
        fun of(widthPx: Int, heightPx: Int, orientation: Orientation): ScreenProfileKey? =
            if (widthPx <= 0 || heightPx <= 0) null
            else ScreenProfileKey(minOf(widthPx, heightPx), maxOf(widthPx, heightPx), orientation)

        fun decode(value: String): ScreenProfileKey? {
            val (size, orientation) = value.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
            val (short, long) = size.split('x', limit = 2).takeIf { it.size == 2 } ?: return null
            return ScreenProfileKey(
                shortPx = short.toIntOrNull()?.takeIf { it > 0 } ?: return null,
                longPx = long.toIntOrNull()?.takeIf { it > 0 } ?: return null,
                orientation = runCatching { Orientation.valueOf(orientation) }.getOrNull() ?: return null
            )
        }
    }
}

/** Most calibrations kept; past this the oldest is forgotten (it falls back to the template again). */
const val MAX_SCREEN_PROFILES = 12

/** "key;offsetX;offsetY;width;height;corner" per line. Malformed lines are skipped. */
internal fun encodeProfiles(profiles: Map<ScreenProfileKey, Geometry>): String =
    profiles.entries.joinToString("\n") { (key, g) ->
        listOf(key.encode(), g.offsetXDp, g.offsetYDp, g.widthDp, g.heightDp, g.cornerDp).joinToString(";")
    }

internal fun decodeProfiles(value: String?): Map<ScreenProfileKey, Geometry> {
    val result = linkedMapOf<ScreenProfileKey, Geometry>()
    value.orEmpty().lineSequence().forEach { line ->
        val f = line.split(';')
        if (f.size != 6) return@forEach
        val key = ScreenProfileKey.decode(f[0]) ?: return@forEach
        val n = f.drop(1).map { it.toIntOrNull() ?: return@forEach }
        result[key] = Geometry(n[0], n[1], n[2], n[3], n[4])
    }
    return result
}

enum class ColorSource { DYNAMIC, BRAND }

/** Activity cards, highest priority first (spec §2.3). The order the user starts with (F-06). */
val DefaultCardPriority: List<CapsuleCardType> = listOf(
    CapsuleCardType.NOTIFICATION,
    CapsuleCardType.TIMER,
    CapsuleCardType.MEDIA,
    CapsuleCardType.PROGRESS,
    CapsuleCardType.CHARGING,
    CapsuleCardType.SLIDERS,
    CapsuleCardType.SHORTCUTS
)

/**
 * Moves [card] by [delta] places among the reorderable cards only, so Controls and Shortcuts stay
 * behind them. A move past either end, or of a card that cannot be moved, changes nothing.
 */
fun moveCard(order: List<CapsuleCardType>, card: CapsuleCardType, delta: Int): List<CapsuleCardType> {
    val full = normalizeCardPriority(order)
    val movable = full.filter { it in ReorderableCards }.toMutableList()
    val from = movable.indexOf(card)
    val to = from + delta
    if (from == -1 || to !in movable.indices) return full
    movable.add(to, movable.removeAt(from))
    return movable + full.filterNot { it in ReorderableCards }
}

/** The cards whose position the user can change; Controls and Shortcuts are opened on request. */
val ReorderableCards: List<CapsuleCardType> = DefaultCardPriority.take(5)

/**
 * Makes any stored order usable: duplicates are dropped and a card that is missing (stored by an
 * older build, or added later) keeps its default position relative to the others by being appended
 * in default order. The result always contains every card exactly once.
 */
fun normalizeCardPriority(order: List<CapsuleCardType>): List<CapsuleCardType> {
    val seen = order.distinct()
    return seen + DefaultCardPriority.filterNot { it in seen }
}

/**
 * F-81: outline of the capsule. ROUNDED_RECT uses the geometry's corner radius (what the capsule
 * always did, so it is the default), PILL is fully round whatever the radius says, SQUIRCLE has
 * the same radius with continuous, slightly squarer corners.
 */
enum class CapsuleShapeStyle { PILL, ROUNDED_RECT, SQUIRCLE }

/** F-82: how much the capsule overshoots when it opens. NONE settles without overshoot. */
enum class BounceLevel { NONE, LOW, DEFAULT, HIGH, MAX }

/** F-82: ORGANIC springs open and shut, RIGID is a short fixed-time ease. */
enum class ExpandStyle { ORGANIC, RIGID }

const val MIN_ANIMATION_SPEED = 0.5f
const val MAX_ANIMATION_SPEED = 2f
const val MAX_BORDER_DP = 3
/** The overlay window only leaves a small margin around the capsule, so a bigger shadow is clipped. */
const val MAX_SHADOW_DP = 6

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
    /**
     * Geometry used for a screen that has no profile of its own yet (F-08). These are the calibrations
     * from before profiles existed, so nobody has to recalibrate; a new profile starts from them.
     */
    val portrait: Geometry = Geometry(),
    val landscape: Geometry = Geometry(),
    /** F-08: the calibration of each panel x orientation the user has adjusted. */
    val profiles: Map<ScreenProfileKey, Geometry> = emptyMap(),
    val idleOpacity: Float = 1f,
    val alwaysShowPill: Boolean = true,
    val showCalibrationTarget: Boolean = false,
    val colorSource: ColorSource = ColorSource.DYNAMIC,
    val hapticsEnabled: Boolean = true,
    /** F-06: which activity card comes first when several are active. Always a full permutation. */
    val cardPriority: List<CapsuleCardType> = DefaultCardPriority,
    val shapeStyle: CapsuleShapeStyle = CapsuleShapeStyle.ROUNDED_RECT,
    /** F-81: ARGB of the capsule background; null = the black shell that blends with the camera. */
    val customBackground: Long? = null,
    val borderDp: Int = 0,
    /** ARGB of the border; null = the theme's outline color. */
    val borderColor: Long? = null,
    val shadowDp: Int = 0,
    val animationSpeed: Float = 1f,
    val bounce: BounceLevel = BounceLevel.DEFAULT,
    val expandStyle: ExpandStyle = ExpandStyle.ORGANIC,
    val reduceAnimations: Boolean = false,
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

    /** The orientation's template, used when [profile] is unknown or has no calibration yet. */
    fun geometry(orientation: Orientation): Geometry =
        if (orientation == Orientation.LANDSCAPE) landscape else portrait

    fun geometry(profile: ScreenProfileKey?, orientation: Orientation): Geometry =
        profile?.let { profiles[it] } ?: geometry(orientation)

    /**
     * Saves [geometry] as the calibration of [profile]. With no profile (screen size not known) the
     * orientation's template is changed instead.
     */
    fun withGeometry(profile: ScreenProfileKey?, orientation: Orientation, geometry: Geometry): Settings {
        if (profile == null) {
            return if (orientation == Orientation.LANDSCAPE) copy(landscape = geometry) else copy(portrait = geometry)
        }
        // Re-inserting moves the profile to the end, so the oldest one is what gets dropped.
        val updated = (profiles - profile + (profile to geometry)).entries.toList().takeLast(MAX_SCREEN_PROFILES)
        return copy(profiles = updated.associate { it.key to it.value })
    }

    /** Forgets [profile]'s calibration; it goes back to the template. */
    fun withoutProfile(profile: ScreenProfileKey): Settings = copy(profiles = profiles - profile)

    fun gesture(type: GestureType, active: Boolean): GestureBinding =
        if (active) gesturesActive[type] ?: DefaultActiveGestures.getValue(type)
        else gesturesIdle[type] ?: DefaultIdleGestures.getValue(type)
}
