package com.iqbalwork.capsule.engine.state

import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.CutoutPreferences
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TimerState

/**
 * All user actions and system event signals dispatchable to the CapsuleStateEngine.
 */
sealed class UserAction {
    // 5-Way Core Gestures
    data object Tap : UserAction()
    data object DoubleTap : UserAction()
    data object LongPress : UserAction()
    data object SwipeLeft : UserAction()
    data object SwipeRight : UserAction()
    data object Dismiss : UserAction()

    // Card Stack Selection
    data class SelectCard(val cardType: CapsuleCardType) : UserAction()
    data class ToggleCardEnabled(val cardType: CapsuleCardType, val enabled: Boolean) : UserAction()

    // Media Controls
    data object MediaPlayPause : UserAction()
    data object MediaNext : UserAction()
    data object MediaPrevious : UserAction()
    data class MediaSeek(val positionMs: Long) : UserAction()

    // Slider Controls
    data class SetVolume(val level: Float) : UserAction()
    data object CycleVolumeStyle : UserAction()
    data object CycleRingerMode : UserAction()
    data class SetBrightness(val level: Float) : UserAction()
    data object ToggleTorch : UserAction()
    data class SetTorchLevel(val level: Float) : UserAction()

    // Notification Controls
    data class ReplyNotification(val id: String, val replyText: String) : UserAction()
    data class DismissNotification(val id: String) : UserAction()

    // Timer Controls
    data object TimerAddMinute : UserAction()
    data object TimerTogglePause : UserAction()
    data object TimerReset : UserAction()

    // Shortcuts
    data class ExecuteShortcut(val shortcutId: String) : UserAction()

    // State Synchronization from Listeners / System
    data class UpdateMedia(val mediaState: MediaState) : UserAction()
    data class UpdateSliders(val sliderState: SliderState) : UserAction()
    data class UpdateCharging(val chargingState: ChargingState) : UserAction()
    data class UpdateNotification(val notificationState: NotificationState?) : UserAction()
    data class UpdateTimer(val timerState: TimerState) : UserAction()
    data class TriggerMiniEvent(val event: MiniEvent) : UserAction()
    data object DismissMiniEvent : UserAction()
    data class UpdateCutoutPreferences(val preferences: CutoutPreferences) : UserAction()
}
