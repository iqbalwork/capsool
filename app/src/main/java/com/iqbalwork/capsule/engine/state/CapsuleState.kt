package com.iqbalwork.capsule.engine.state

import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.CutoutPreferences
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.ShortcutsState
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TimerState

/**
 * Single source of truth immutable state for the capsule system.
 */
data class CapsuleState(
    val isExpanded: Boolean = false,
    val activeCard: CapsuleCardType = CapsuleCardType.MEDIA,
    val availableCards: List<CapsuleCardType> = listOf(
        CapsuleCardType.MEDIA,
        CapsuleCardType.SLIDERS,
        CapsuleCardType.CHARGING,
        CapsuleCardType.NOTIFICATION,
        CapsuleCardType.TIMER,
        CapsuleCardType.SHORTCUTS
    ),
    val activeCardIndex: Int = 0,
    val mediaState: MediaState = MediaState(),
    val sliderState: SliderState = SliderState(),
    val chargingState: ChargingState = ChargingState(),
    val notificationState: NotificationState? = null,
    val timerState: TimerState = TimerState(),
    val shortcutsState: ShortcutsState = ShortcutsState(),
    val activeMiniEvent: MiniEvent? = null,
    val cutoutPreferences: CutoutPreferences = CutoutPreferences()
)
