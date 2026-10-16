package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.CapsuleNotification
import com.iqbalwork.capsule.model.ProgressItem
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TimerState

sealed interface Action

/** Something the user did on the capsule or in the companion app. */
sealed interface UserIntent : Action {
    data class Gesture(val type: GestureType) : UserIntent
    /**
     * Runs a gesture action directly, without going through the user's gesture bindings. Used by
     * accessibility actions (TalkBack cannot swipe or long press), which must do a fixed thing.
     */
    data class Perform(val action: GestureAction) : UserIntent
    data object SwipeUp : UserIntent
    data object Collapse : UserIntent
    data class SelectCard(val card: CapsuleCardType) : UserIntent

    data object MediaPlayPause : UserIntent
    data object MediaNext : UserIntent
    data object MediaPrevious : UserIntent
    data class MediaSeek(val positionMs: Long) : UserIntent
    data object MediaOpenApp : UserIntent

    data class SetVolume(val level: Float) : UserIntent
    data object CycleRinger : UserIntent
    data class SetBrightness(val level: Float) : UserIntent
    data object ToggleAdaptiveBrightness : UserIntent
    data object ToggleTorch : UserIntent
    data class SetTorchLevel(val level: Float) : UserIntent

    data class NotificationStartReply(val key: String) : UserIntent
    data object NotificationCancelReply : UserIntent
    data class NotificationReply(val key: String, val text: String) : UserIntent
    data class NotificationMarkRead(val key: String) : UserIntent
    data class NotificationDismiss(val key: String) : UserIntent
    data class NotificationOpen(val key: String) : UserIntent

    /** Runs the clock app's own notification button at [index] (+1 min, pause, stop…). */
    data class TimerAction(val key: String, val index: Int) : UserIntent

    data class LaunchShortcut(val id: String) : UserIntent

    /** Fires one of the user's custom intents from a "Custom action" static card (F-76). */
    data class RunCustomIntent(val id: String) : UserIntent
}

/** Something the system (or a settings change) reported. */
sealed interface SystemEvent : Action {
    data class SettingsChanged(val settings: Settings) : SystemEvent
    data class VisibilityChanged(val visibility: VisibilityContext) : SystemEvent
    data class MediaUpdated(val media: MediaState?) : SystemEvent
    data class MediaPauseTimedOut(val packageName: String, val title: String) : SystemEvent
    data class SlidersUpdated(val sliders: SliderState) : SystemEvent
    data class ChargingUpdated(val charging: ChargingState) : SystemEvent
    /** [alert] false = an update that refreshes an existing entry without popping up again. */
    data class NotificationPosted(val notification: CapsuleNotification, val alert: Boolean = true) : SystemEvent
    data class NotificationRemoved(val key: String) : SystemEvent
    data class NotificationTimedOut(val key: String) : SystemEvent
    data class TimerUpdated(val timer: TimerState?) : SystemEvent
    data class ProgressUpdated(val item: ProgressItem) : SystemEvent
    data class ProgressRemoved(val key: String) : SystemEvent
    data class MiniEventStarted(val event: MiniEvent) : SystemEvent
    data class MiniEventFinished(val id: Long) : SystemEvent
    data object ExpandedTimedOut : SystemEvent
    /** The screenshot was taken with the capsule hidden; show it again. */
    data object ScreenshotDone : SystemEvent
}
