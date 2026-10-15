package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.CapsuleNotification
import com.iqbalwork.capsule.model.ProgressItem
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
    CapsuleCardType.PROGRESS,
    CapsuleCardType.CHARGING,
    CapsuleCardType.SLIDERS,
    CapsuleCardType.SHORTCUTS
)

data class CapsuleState(
    val settingsLoaded: Boolean = false,
    val settings: Settings = Settings(),
    val visibility: VisibilityContext = VisibilityContext(),
    val isExpanded: Boolean = false,
    /**
     * Key of the focused page ([CardPage.key]).  Stored as a string so it can address both
     * dynamic pages ("dyn:MEDIA") and static pages ("static:<id>").
     */
    val focusedPageKey: String? = null,
    val miniEvent: MiniEvent? = null,
    val media: MediaState? = null,
    val sliders: SliderState = SliderState(),
    val charging: ChargingState = ChargingState(),
    /** Newest first. The card shows the first one. */
    val notifications: List<CapsuleNotification> = emptyList(),
    /** A card opened on request (Controls, Shortcuts) rather than by activity; closes with the card. */
    val openedCard: CapsuleCardType? = null,
    /** Hidden for a moment so a screenshot does not include the capsule. */
    val screenshotPending: Boolean = false,
    /** Key of the notification being replied to; the overlay takes keyboard focus meanwhile. */
    val replyingTo: String? = null,
    val timer: TimerState? = null,
    /** In the order they started; the card shows the first few. */
    val progress: List<ProgressItem> = emptyList()
) {
    /**
     * Unified page list for the carousel.
     *
     * - If there are active dynamic cards → one [CardPage.Dynamic] per card, priority-ordered.
     * - Otherwise → one [CardPage.Static] per entry in [Settings.effectiveStaticCards].
     */
    val pages: List<CardPage>
        get() {
            val dynamic = CardPriority.filter { card ->
                when (card) {
                    CapsuleCardType.NOTIFICATION -> settings.notificationsEnabled && notifications.isNotEmpty()
                    CapsuleCardType.TIMER -> settings.timerEnabled && timer != null
                    CapsuleCardType.PROGRESS -> settings.progressEnabled && progress.isNotEmpty()
                    CapsuleCardType.MEDIA -> mediaVisible
                    CapsuleCardType.CHARGING -> settings.chargingCardEnabled && charging.isCharging
                    CapsuleCardType.SLIDERS -> openedCard == CapsuleCardType.SLIDERS || sliders.isTorchOn
                    CapsuleCardType.SHORTCUTS -> openedCard == CapsuleCardType.SHORTCUTS
                }
            }.map { CardPage.Dynamic(it) }

            if (dynamic.isNotEmpty()) return dynamic

            return settings.effectiveStaticCards.map { CardPage.Static(it) }
        }

    /** The page the carousel shows: the focused one (by key) if still in [pages]; otherwise the
     *  first page.  For static-only mode the focus defaults to [Settings.effectiveStartCardId]. */
    val activePage: CardPage?
        get() {
            val pageList = pages
            if (pageList.isEmpty()) return null
            val key = focusedPageKey ?: effectiveStaticStartKey
            return pageList.firstOrNull { it.key == key } ?: pageList.first()
        }

    /** Cards that currently have something real to show, in priority order (dynamic only). */
    val activeCards: List<CapsuleCardType>
        get() = pages.filterIsInstance<CardPage.Dynamic>().map { it.type }

    /** Media card rules (F-10, F-12): enabled, not parked after a pause, its app not in front. */
    val mediaVisible: Boolean
        get() {
            val current = media ?: return false
            return settings.mediaEnabled &&
                !current.hiddenAfterPause &&
                !(settings.hideMediaWhenAppInFront && visibility.foregroundPackage == current.packageName)
        }

    val topNotification: CapsuleNotification? get() = notifications.firstOrNull()

    /**
     * F-75: gestures use the "active" set while something besides an opened card is showing.
     * Static pages are idle context — they do not count as activity.
     */
    val hasActivity: Boolean get() = activeCards.any { it != openedCard }

    fun gesture(type: GestureType): GestureBinding = settings.gesture(type, active = hasActivity)

    /** F-22: on the lock screen only public notifications show their text, if the user asks for that. */
    fun showsText(notification: CapsuleNotification): Boolean =
        !(visibility.locked && settings.hideNotificationTextWhenLocked && !notification.isPublic)

    /**
     * The dynamic card the carousel shows (derived from [activePage] for backward compatibility).
     * Returns null when the active page is a static card.
     */
    val activeCard: CapsuleCardType?
        get() = (activePage as? CardPage.Dynamic)?.type

    /** The effectiveStartCardId expressed as a page key, used for initial focus in static mode. */
    private val effectiveStaticStartKey: String?
        get() = settings.effectiveStartCardId?.let { "static:$it" }

    val isAllowedOnScreen: Boolean
        get() = settingsLoaded &&
            !screenshotPending &&
            visibility.screenOn &&
            !(settings.hideInFullscreen && visibility.fullscreen) &&
            // Our own reply field opens the keyboard; that must not hide the capsule.
            !(settings.hideWithKeyboard && visibility.imeVisible && replyingTo == null) &&
            !(settings.hideInLandscape && visibility.orientation == Orientation.LANDSCAPE)

    val mode: CapsuleMode
        get() = when {
            !isAllowedOnScreen -> CapsuleMode.HIDDEN
            isExpanded && pages.isNotEmpty() -> CapsuleMode.EXPANDED
            miniEvent != null -> CapsuleMode.MINI_EVENT
            activeCards.isNotEmpty() -> CapsuleMode.COMPACT
            settings.alwaysShowPill || settings.showCalibrationTarget -> CapsuleMode.IDLE
            else -> CapsuleMode.HIDDEN
        }
}
