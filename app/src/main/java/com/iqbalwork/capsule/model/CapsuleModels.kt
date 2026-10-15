package com.iqbalwork.capsule.model

enum class CapsuleCardType(val displayName: String) {
    MEDIA("Media Player"),
    SLIDERS("System Sliders"),
    CHARGING("Battery & Charging"),
    NOTIFICATION("Notifications"),
    TIMER("Timer & Stopwatch"),
    SHORTCUTS("Shortcuts & Barcode")
}

data class MediaState(
    val title: String = "Not Playing",
    val artist: String = "Unknown Artist",
    val albumArtUri: String? = null,
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 180000L,
    val dominantColor: Long = 0xFF733512
)

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
    val volumeLevel: Float = 0.65f,
    val volumeStyle: VolumeSliderStyle = VolumeSliderStyle.ANDROID_15,
    val ringerMode: RingerMode = RingerMode.NORMAL,
    val brightnessLevel: Float = 0.7f,
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
    val chargePercentage: Int = 85,
    val chargeType: ChargingType = ChargingType.NONE,
    val isFullCharged: Boolean = false,
    val batteryTemperatureC: Float = 31.5f,
    val isLowBattery: Boolean = false
)

data class NotificationActionItem(
    val id: String,
    val title: String,
    val isReply: Boolean = false
)

data class NotificationState(
    val id: String = "demo_1",
    val title: String = "WhatsApp",
    val content: String = "Alex: Hey, are we still meeting today?",
    val packageName: String = "com.whatsapp",
    val appName: String = "WhatsApp",
    val senderAvatarUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val glowColor: Long = 0xFF25D366,
    val actions: List<NotificationActionItem> = listOf(
        NotificationActionItem("reply", "Reply", isReply = true),
        NotificationActionItem("read", "Mark as read"),
        NotificationActionItem("dismiss", "Dismiss")
    )
)

data class TimerState(
    val remainingSeconds: Int = 245,
    val totalSeconds: Int = 300,
    val isRunning: Boolean = true,
    val label: String = "Work Timer"
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
    val shortcuts: List<ShortcutItem> = listOf(
        ShortcutItem("chatgpt", "ChatGPT", "Instant Prompt", ShortcutType.CHATGPT, "chatgpt"),
        ShortcutItem("wifi", "Wi-Fi", "Connected", ShortcutType.SYSTEM, "wifi"),
        ShortcutItem("bluetooth", "Bluetooth", "On", ShortcutType.SYSTEM, "bluetooth"),
        ShortcutItem("flashlight", "Torch", "Toggle", ShortcutType.SYSTEM, "flashlight"),
        ShortcutItem("whatsapp", "WhatsApp", "Quick Chat", ShortcutType.APP, "whatsapp"),
        ShortcutItem("member_barcode", "Member Card", "EAN-13: 8991234567890", ShortcutType.BARCODE, "barcode")
    )
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
    val type: MiniEventType,
    val title: String,
    val subtitle: String = "",
    val durationMs: Long = 3500L
)

enum class CutoutAlignment {
    CENTER,
    LEFT,
    RIGHT,
    CUSTOM
}

data class CutoutPreferences(
    val alignment: CutoutAlignment = CutoutAlignment.CENTER,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val capsuleWidthDp: Int = 120,
    val capsuleHeightDp: Int = 36,
    val cornerRadiusDp: Int = 18,
    val screenCornerRadiusDp: Int = 0,
    val idleOpacity: Float = 1.0f,
    val showRedTarget: Boolean = false
)
