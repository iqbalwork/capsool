package com.iqbalwork.capsule.model

import android.graphics.Bitmap

enum class CapsuleCardType(val displayName: String) {
    MEDIA("Media Player"),
    SLIDERS("System Sliders"),
    CHARGING("Battery & Charging"),
    NOTIFICATION("Notifications"),
    TIMER("Timer & Stopwatch"),
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

enum class VolumeSliderStyle(val displayName: String) {
    ANDROID_15("Android 15 (Single Pill)"),
    ANDROID_16("Android 16 (Split Segmented)"),
    SAMSUNG_ONE_UI("Samsung One UI (Rounded)")
}

enum class RingerMode(val displayName: String) {
    NORMAL("Ring"),
    VIBRATE("Vibrate"),
    SILENT("Silent / DND")
}

data class SliderState(
    val volumeLevel: Float = 0f,
    val volumeStyle: VolumeSliderStyle = VolumeSliderStyle.ANDROID_15,
    val ringerMode: RingerMode = RingerMode.NORMAL,
    val brightnessLevel: Float = 0f,
    val isAdaptiveBrightness: Boolean = false,
    val isTorchOn: Boolean = false,
    val torchStrengthLevel: Float = 1.0f
)

enum class ChargingType {
    AC,
    USB,
    WIRELESS,
    NONE
}

data class ChargingState(
    val isCharging: Boolean = false,
    val chargePercentage: Int = 0,
    val chargeType: ChargingType = ChargingType.NONE,
    val isFullCharged: Boolean = false,
    val batteryTemperatureC: Float = 0f,
    val isLowBattery: Boolean = false
)

data class NotificationActionItem(
    val id: String,
    val title: String,
    val isReply: Boolean = false
)

data class NotificationState(
    val id: String,
    val title: String,
    val content: String,
    val packageName: String,
    val appName: String,
    val senderAvatarUri: String? = null,
    val timestamp: Long,
    val glowColor: Long,
    val actions: List<NotificationActionItem> = emptyList()
)

data class TimerState(
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean,
    val label: String
)

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

enum class MiniEventType {
    TWS_CONNECTED,
    CHARGER_CONNECTED,
    RINGER_CHANGED,
    WIFI_CHANGED,
    AIRPLANE_MODE,
    DEVICE_UNLOCKED,
    LOW_BATTERY
}

data class MiniEvent(
    val id: Long,
    val type: MiniEventType,
    val title: String,
    val subtitle: String = "",
    val durationMs: Long = 3500L
)
