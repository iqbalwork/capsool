package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TimerState

sealed interface Action

/** Something the user did on the capsule or in the companion app. */
sealed interface UserIntent : Action {
    data class Gesture(val type: GestureType) : UserIntent
    data object SwipeUp : UserIntent
    data object Collapse : UserIntent
    data class SelectCard(val card: CapsuleCardType) : UserIntent

    data object MediaPlayPause : UserIntent
    data object MediaNext : UserIntent
    data object MediaPrevious : UserIntent
    data class MediaSeek(val positionMs: Long) : UserIntent
    data object MediaOpenApp : UserIntent

    data class SetVolume(val level: Float) : UserIntent
    data object CycleVolumeStyle : UserIntent
    data object CycleRinger : UserIntent
    data class SetBrightness(val level: Float) : UserIntent
    data object ToggleTorch : UserIntent
    data class SetTorchLevel(val level: Float) : UserIntent

    data class NotificationReply(val id: String, val text: String) : UserIntent
    data class NotificationDismiss(val id: String) : UserIntent

    data object TimerAddMinute : UserIntent
    data object TimerTogglePause : UserIntent
    data object TimerReset : UserIntent

    data class LaunchShortcut(val id: String) : UserIntent
}

/** Something the system (or a settings change) reported. */
sealed interface SystemEvent : Action {
    data class SettingsChanged(val settings: Settings) : SystemEvent
    data class VisibilityChanged(val visibility: VisibilityContext) : SystemEvent
    data class MediaUpdated(val media: MediaState?) : SystemEvent
    data class MediaPauseTimedOut(val packageName: String, val title: String) : SystemEvent
    data class SlidersUpdated(val sliders: SliderState) : SystemEvent
    data class ChargingUpdated(val charging: ChargingState) : SystemEvent
    data class NotificationUpdated(val notification: NotificationState?) : SystemEvent
    data class TimerUpdated(val timer: TimerState?) : SystemEvent
    data class MiniEventStarted(val event: MiniEvent) : SystemEvent
    data class MiniEventFinished(val id: Long) : SystemEvent
    data object ExpandedTimedOut : SystemEvent
}
