package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.model.RingerMode

/** Side effects the reducer asks the platform layer to perform. */
sealed interface Effect {
    enum class MediaCommand { PLAY_PAUSE, NEXT, PREVIOUS }
    enum class GlobalAction { NOTIFICATIONS, QUICK_SETTINGS }

    /** System screens where the user grants what a control needs. */
    enum class PermissionScreen { WRITE_SETTINGS, DND_ACCESS }

    data class Media(val command: MediaCommand) : Effect
    data class MediaSeek(val positionMs: Long) : Effect
    data object MediaOpenApp : Effect
    data class Volume(val level: Float) : Effect
    data class Ringer(val mode: RingerMode) : Effect
    data class Brightness(val level: Float) : Effect
    data class AdaptiveBrightness(val on: Boolean) : Effect
    data class OpenPermission(val screen: PermissionScreen) : Effect
    /** Re-read control state; permissions granted in Settings send no broadcast. */
    data object RefreshControls : Effect
    data class Torch(val on: Boolean, val level: Float?) : Effect
    data class NotificationReply(val key: String, val text: String) : Effect
    data class NotificationMarkRead(val key: String) : Effect
    data class NotificationDismiss(val key: String) : Effect
    data class NotificationOpen(val key: String) : Effect
    data class Global(val action: GlobalAction) : Effect
    data class LaunchShortcut(val id: String) : Effect
}
