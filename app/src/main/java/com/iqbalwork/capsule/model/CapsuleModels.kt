package com.iqbalwork.capsule.model

import android.graphics.Bitmap

enum class CapsuleCardType(val displayName: String) {
    MEDIA("Media Player"),
    SLIDERS("System Sliders"),
    CHARGING("Battery & Charging"),
    NOTIFICATION("Notifications"),
    TIMER("Timer & Stopwatch"),
    PROGRESS("Downloads & uploads"),
    SHORTCUTS("Shortcuts & Barcode")
}

data class MediaState(
    val packageName: String = "",
    val title: String = "",
    val artist: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    /** `SystemClock.elapsedRealtime()` when [playbackPositionMs] was reported. */
    val positionUpdatedAtMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val durationMs: Long = 0L,
    val canSeek: Boolean = false,
    val dominantColor: Long = 0xFF733512,
    /** Paused long enough (or never seen playing) that the card should step aside. */
    val hiddenAfterPause: Boolean = false
) {
    val showsSeekBar: Boolean get() = canSeek && durationMs > 0

    /** Position extrapolated to [nowElapsedMs], the way the system media controls do it. */
    fun positionAt(nowElapsedMs: Long): Long {
        if (!isPlaying || positionUpdatedAtMs == 0L) return playbackPositionMs
        val elapsed = ((nowElapsedMs - positionUpdatedAtMs) * playbackSpeed).toLong()
        val position = playbackPositionMs + elapsed.coerceAtLeast(0L)
        return if (durationMs > 0) position.coerceAtMost(durationMs) else position
    }

    fun isSameTrack(other: MediaState?): Boolean =
        other != null && other.packageName == packageName && other.title == title && other.artist == artist
}

enum class RingerMode(val displayName: String) {
    NORMAL("Ring"),
    VIBRATE("Vibrate"),
    SILENT("Silent")
}

enum class TorchState {
    /** The device has no usable flash unit. */
    NONE,
    OFF,
    ON,
    /** Another app (usually the camera) holds the flash. */
    BUSY
}

/** System controls shown on the Controls card (F-50..F-53). All levels are 0..1. */
data class SliderState(
    val volumeLevel: Float = 0f,
    val ringerMode: RingerMode = RingerMode.NORMAL,
    /** Notification policy access, needed to switch to or from silent (spec §2.5.5). */
    val canSilence: Boolean = false,
    /** Slider position on the gamma curve, like the system brightness slider. */
    val brightnessLevel: Float = 0f,
    val isAdaptiveBrightness: Boolean = false,
    /** WRITE_SETTINGS granted. */
    val canWriteBrightness: Boolean = false,
    val torch: TorchState = TorchState.NONE,
    val torchStrengthLevel: Float = 1f,
    /** Android 13+ device whose flash supports more than one strength level. */
    val torchStrengthSupported: Boolean = false
) {
    val isTorchOn: Boolean get() = torch == TorchState.ON
}

enum class ChargingType {
    AC,
    USB,
    WIRELESS,
    DOCK,
    NONE
}

data class ChargingState(
    val isCharging: Boolean = false,
    val chargePercentage: Int = 0,
    val chargeType: ChargingType = ChargingType.NONE,
    val isFullCharged: Boolean = false,
    val batteryTemperatureC: Float = 0f,
    val isLowBattery: Boolean = false,
    /** From BatteryManager.computeChargeTimeRemaining(); null when unknown. */
    val chargeTimeRemainingMs: Long? = null
)

/** A notification as the capsule shows it. Bitmaps are already downscaled by the source. */
data class CapsuleNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val sender: String,
    val text: String,
    val conversationTitle: String? = null,
    val avatar: Bitmap? = null,
    /** The app's monochrome status-bar icon; draw it tinted. */
    val appIcon: Bitmap? = null,
    val glowColor: Long,
    val postedAt: Long,
    val canReply: Boolean = false,
    val canMarkRead: Boolean = false,
    val clearable: Boolean = true,
    /** Posted with VISIBILITY_PUBLIC, so its text may show on the lock screen. */
    val isPublic: Boolean = false
)

/** One of the clock app's own notification buttons, run by index (spec §2.5.6). */
data class TimerAction(val index: Int, val title: String)

/**
 * A timer or stopwatch read from a chronometer notification (F-41). Time is derived from
 * [baseTimeMs] (wall clock: the countdown's end, or the stopwatch's start) while running.
 */
data class TimerState(
    val key: String,
    val packageName: String,
    val appName: String,
    val label: String,
    val isCountdown: Boolean,
    val baseTimeMs: Long,
    val isRunning: Boolean,
    /** What the clock app shows while paused (e.g. "4:05"), since no live base time exists then. */
    val staticText: String = "",
    val actions: List<TimerAction> = emptyList(),
    val appIcon: Bitmap? = null
) {
    /** Seconds to show: remaining for a countdown, elapsed for a stopwatch. Never negative. */
    fun displaySeconds(nowMs: Long): Long =
        (if (isCountdown) (baseTimeMs - nowMs + 999) / 1000 else (nowMs - baseTimeMs) / 1000).coerceAtLeast(0)
}

/** A download, upload or other progress notification (F-40). [fraction] null = indeterminate. */
data class ProgressItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val fraction: Float?,
    val appIcon: Bitmap? = null,
    val color: Long
)

/** H:MM:SS above an hour, otherwise M:SS. */
fun formatClock(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

enum class ShortcutType {
    APP,
    SYSTEM,
    CONTACT,
    BARCODE,
    CHATGPT
}

data class ShortcutItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val type: ShortcutType,
    val iconKey: String
)

data class ShortcutsState(
    val shortcuts: List<ShortcutItem> = emptyList()
)

/** Groups of mini events the user can switch on or off (F-60..F-69). */
enum class EventCategory { CHARGING, LOW_BATTERY, RINGER, HEADPHONES, WIFI, AIRPLANE, UNLOCK, BRIGHTNESS_MODE, DOWNLOADS }

enum class MiniEventType(val category: EventCategory) {
    CHARGER_CONNECTED(EventCategory.CHARGING),
    CHARGER_DISCONNECTED(EventCategory.CHARGING),
    FULLY_CHARGED(EventCategory.CHARGING),
    LOW_BATTERY(EventCategory.LOW_BATTERY),
    RINGER_NORMAL(EventCategory.RINGER),
    RINGER_VIBRATE(EventCategory.RINGER),
    RINGER_SILENT(EventCategory.RINGER),
    HEADPHONES_CONNECTED(EventCategory.HEADPHONES),
    HEADPHONES_DISCONNECTED(EventCategory.HEADPHONES),
    WIFI_CONNECTED(EventCategory.WIFI),
    WIFI_DISCONNECTED(EventCategory.WIFI),
    AIRPLANE_ON(EventCategory.AIRPLANE),
    AIRPLANE_OFF(EventCategory.AIRPLANE),
    DEVICE_UNLOCKED(EventCategory.UNLOCK),
    AUTO_BRIGHTNESS_ON(EventCategory.BRIGHTNESS_MODE),
    AUTO_BRIGHTNESS_OFF(EventCategory.BRIGHTNESS_MODE),
    PROGRESS_DONE(EventCategory.DOWNLOADS)
}

/** A 2-4 second event in the capsule. [level] is a 0-100 value shown as a ring (battery). */
data class MiniEvent(
    val id: Long,
    val type: MiniEventType,
    val title: String,
    val subtitle: String = "",
    val level: Int? = null
)
