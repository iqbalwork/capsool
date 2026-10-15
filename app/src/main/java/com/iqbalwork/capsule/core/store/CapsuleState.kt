package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.CapsuleNotification
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TimerState

enum class CapsuleMode { HIDDEN, IDLE, COMPACT, MINI_EVENT, EXPANDED }

/** Device conditions that decide whether the capsule may be shown. */
data class VisibilityContext(
    val screenOn: Boolean = true,
    val fullscreen: Boolean = false,
    val imeVisible: Boolean = false,
    val orientation: Orientation = Orientation.PORTRAIT,
    val foregroundPackage: String? = null,
    val locked: Boolean = false
)

/** Most notifications the capsule keeps at once (spec §2.5.2). */
const val MAX_CAPSULE_NOTIFICATIONS = 5

/** Activity cards, highest priority first (spec §2.3). */
private val CardPriority = listOf(
    CapsuleCardType.NOTIFICATION,
    CapsuleCardType.TIMER,
    CapsuleCardType.MEDIA,
    CapsuleCardType.CHARGING
)

data class CapsuleState(
    val settingsLoaded: Boolean = false,
    val settings: Settings = Settings(),
    val visibility: VisibilityContext = VisibilityContext(),
    val isExpanded: Boolean = false,
    val focusedCard: CapsuleCardType? = null,
    val miniEvent: MiniEvent? = null,
    val media: MediaState? = null,
    val sliders: SliderState = SliderState(),
    val charging: ChargingState = ChargingState(),
    /** Newest first. The card shows the first one. */
    val notifications: List<CapsuleNotification> = emptyList(),
    /** Key of the notification being replied to; the overlay takes keyboard focus meanwhile. */
    val replyingTo: String? = null,
    val timer: TimerState? = null
) {
    /** Cards that currently have something real to show, in priority order. */
    val activeCards: List<CapsuleCardType>
        get() = CardPriority.filter { card ->
            when (card) {
                CapsuleCardType.NOTIFICATION -> settings.notificationsEnabled && notifications.isNotEmpty()
                CapsuleCardType.TIMER -> timer != null
                CapsuleCardType.MEDIA -> mediaVisible
                CapsuleCardType.CHARGING -> charging.isCharging
                else -> false
            }
        }

    /** Media card rules (F-10, F-12): enabled, not parked after a pause, its app not in front. */
    val mediaVisible: Boolean
        get() {
            val current = media ?: return false
            return settings.mediaEnabled &&
                !current.hiddenAfterPause &&
                !(settings.hideMediaWhenAppInFront && visibility.foregroundPackage == current.packageName)
        }

    val topNotification: CapsuleNotification? get() = notifications.firstOrNull()

    /** F-22: on the lock screen only public notifications show their text, if the user asks for that. */
    fun showsText(notification: CapsuleNotification): Boolean =
        !(visibility.locked && settings.hideNotificationTextWhenLocked && !notification.isPublic)

    /** The card the carousel shows: the focused one if still active, otherwise the top one. */
    val activeCard: CapsuleCardType?
        get() = activeCards.let { cards -> focusedCard?.takeIf { it in cards } ?: cards.firstOrNull() }

    val isAllowedOnScreen: Boolean
        get() = settingsLoaded &&
            visibility.screenOn &&
            !(settings.hideInFullscreen && visibility.fullscreen) &&
            // Our own reply field opens the keyboard; that must not hide the capsule.
            !(settings.hideWithKeyboard && visibility.imeVisible && replyingTo == null) &&
            !(settings.hideInLandscape && visibility.orientation == Orientation.LANDSCAPE)

    val mode: CapsuleMode
        get() = when {
            !isAllowedOnScreen -> CapsuleMode.HIDDEN
            isExpanded && activeCards.isNotEmpty() -> CapsuleMode.EXPANDED
            miniEvent != null -> CapsuleMode.MINI_EVENT
            activeCards.isNotEmpty() -> CapsuleMode.COMPACT
            settings.alwaysShowPill || settings.showCalibrationTarget -> CapsuleMode.IDLE
            else -> CapsuleMode.HIDDEN
        }
}
