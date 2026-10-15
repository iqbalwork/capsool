package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.model.RingerMode

/** Side effects the reducer asks the platform layer to perform. */
sealed interface Effect {
    enum class MediaCommand { PLAY_PAUSE, NEXT, PREVIOUS }
    enum class GlobalAction { NOTIFICATIONS, QUICK_SETTINGS }

    data class Media(val command: MediaCommand) : Effect
    data class MediaSeek(val positionMs: Long) : Effect
    data object MediaOpenApp : Effect
    data class Volume(val level: Float) : Effect
    data class Ringer(val mode: RingerMode) : Effect
    data class Brightness(val level: Float) : Effect
    data class Torch(val on: Boolean, val level: Float?) : Effect
    data class NotificationReply(val key: String, val text: String) : Effect
    data class NotificationMarkRead(val key: String) : Effect
    data class NotificationDismiss(val key: String) : Effect
    data class NotificationOpen(val key: String) : Effect
    data class Global(val action: GlobalAction) : Effect
    data class LaunchShortcut(val id: String) : Effect
}
