package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.NotificationDisplay
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.TorchState

/** Torch strengths below this turn the flashlight off instead. */
private const val TORCH_OFF_BELOW = 0.05f

data class Reduction(val state: CapsuleState, val effects: List<Effect> = emptyList())

/**
 * Pure state transitions. Never touches Android APIs: anything that has to reach the
 * system is returned as an [Effect], and the system's answer comes back as a [SystemEvent].
 * Optimistic updates are allowed so controls feel instant; the next system event wins.
 */
object CapsuleReducer {

    fun reduce(state: CapsuleState, action: Action): Reduction {
        val reduction = when (action) {
            is UserIntent -> reduceIntent(state, action)
            SystemEvent.ExpandedTimedOut -> collapse(state)
            is SystemEvent -> Reduction(reduceEvent(state, action))
        }
        return reduction.copy(state = normalize(reduction.state))
    }

    private fun reduceIntent(state: CapsuleState, intent: UserIntent): Reduction = when (intent) {
        is UserIntent.Gesture -> reduceGesture(state, state.settings.gestureAction(intent.type))
        UserIntent.SwipeUp, UserIntent.Collapse -> collapse(state)
        is UserIntent.SelectCard -> Reduction(state.copy(focusedCard = intent.card))

        UserIntent.MediaPlayPause -> playPause(state)
        UserIntent.MediaNext -> Reduction(state, listOf(Effect.Media(Effect.MediaCommand.NEXT)))
        UserIntent.MediaPrevious -> Reduction(state, listOf(Effect.Media(Effect.MediaCommand.PREVIOUS)))
        UserIntent.MediaOpenApp -> Reduction(state.copy(isExpanded = false), listOf(Effect.MediaOpenApp))
        is UserIntent.MediaSeek -> {
            val media = state.media ?: return Reduction(state)
            val position = intent.positionMs.coerceIn(0L, media.durationMs.coerceAtLeast(0L))
            Reduction(state.copy(media = media.copy(playbackPositionMs = position)), listOf(Effect.MediaSeek(position)))
        }

        is UserIntent.SetVolume -> {
            val level = intent.level.coerceIn(0f, 1f)
            Reduction(state.copy(sliders = state.sliders.copy(volumeLevel = level)), listOf(Effect.Volume(level)))
        }
        UserIntent.CycleRinger -> cycleRinger(state)
        is UserIntent.SetBrightness ->
            if (!state.sliders.canWriteBrightness) {
                askPermission(state, Effect.PermissionScreen.WRITE_SETTINGS)
            } else {
                val level = intent.level.coerceIn(0f, 1f)
                Reduction(state.copy(sliders = state.sliders.copy(brightnessLevel = level)), listOf(Effect.Brightness(level)))
            }
        UserIntent.ToggleAdaptiveBrightness ->
            if (!state.sliders.canWriteBrightness) {
                askPermission(state, Effect.PermissionScreen.WRITE_SETTINGS)
            } else {
                val on = !state.sliders.isAdaptiveBrightness
                Reduction(state.copy(sliders = state.sliders.copy(isAdaptiveBrightness = on)), listOf(Effect.AdaptiveBrightness(on)))
            }
        UserIntent.ToggleTorch -> when (state.sliders.torch) {
            TorchState.OFF -> Reduction(
                state.copy(sliders = state.sliders.copy(torch = TorchState.ON)),
                listOf(Effect.Torch(on = true, level = state.sliders.torchStrengthLevel.takeIf { state.sliders.torchStrengthSupported }))
            )
            TorchState.ON -> Reduction(state.copy(sliders = state.sliders.copy(torch = TorchState.OFF)), listOf(Effect.Torch(false, null)))
            TorchState.NONE, TorchState.BUSY -> Reduction(state)
        }
        is UserIntent.SetTorchLevel ->
            if (state.sliders.torch == TorchState.NONE || state.sliders.torch == TorchState.BUSY) {
                Reduction(state)
            } else {
                val level = intent.level.coerceIn(0f, 1f)
                val on = level > TORCH_OFF_BELOW
                Reduction(
                    state.copy(sliders = state.sliders.copy(torchStrengthLevel = level, torch = if (on) TorchState.ON else TorchState.OFF)),
                    listOf(Effect.Torch(on, level.takeIf { on }))
                )
            }

        is UserIntent.NotificationStartReply -> {
            val target = state.notifications.firstOrNull { it.key == intent.key }
            if (target == null || !target.canReply || state.visibility.locked) Reduction(state)
            else Reduction(state.copy(replyingTo = intent.key))
        }
        UserIntent.NotificationCancelReply -> Reduction(state.copy(replyingTo = null))
        is UserIntent.NotificationReply -> Reduction(
            state.withoutNotification(intent.key).copy(replyingTo = null, isExpanded = false),
            listOf(Effect.NotificationReply(intent.key, intent.text))
        )
        is UserIntent.NotificationMarkRead ->
            Reduction(state.withoutNotification(intent.key), listOf(Effect.NotificationMarkRead(intent.key)))
        is UserIntent.NotificationDismiss ->
            Reduction(state.withoutNotification(intent.key), listOf(Effect.NotificationDismiss(intent.key)))
        is UserIntent.NotificationOpen -> Reduction(
            state.withoutNotification(intent.key).copy(isExpanded = false),
            listOf(Effect.NotificationOpen(intent.key))
        )

        // No optimistic update: the clock app answers with an updated notification.
        is UserIntent.TimerAction -> Reduction(state, listOf(Effect.NotificationAction(intent.key, intent.index)))

        is UserIntent.LaunchShortcut -> Reduction(state.copy(isExpanded = false), listOf(Effect.LaunchShortcut(intent.id)))
    }

    private fun reduceGesture(state: CapsuleState, action: GestureAction): Reduction = when (action) {
        GestureAction.NONE -> Reduction(state)
        GestureAction.TOGGLE_EXPAND ->
            // With nothing going on, a tap opens the Controls card instead of doing nothing.
            if (state.activeCards.isEmpty()) openControls(state)
            else if (state.isExpanded) collapse(state)
            else Reduction(state.copy(isExpanded = true, miniEvent = null))
        GestureAction.NEXT_CARD -> Reduction(cycleCard(state, step = 1))
        GestureAction.PREVIOUS_CARD -> Reduction(cycleCard(state, step = -1))
        GestureAction.MEDIA_PLAY_PAUSE -> playPause(state)
        GestureAction.OPEN_CONTROLS -> if (state.isExpanded && state.activeCard == CapsuleCardType.SLIDERS) collapse(state) else openControls(state)
        GestureAction.OPEN_NOTIFICATIONS ->
            Reduction(state.copy(isExpanded = false), listOf(Effect.Global(Effect.GlobalAction.NOTIFICATIONS)))
        GestureAction.OPEN_QUICK_SETTINGS ->
            Reduction(state.copy(isExpanded = false), listOf(Effect.Global(Effect.GlobalAction.QUICK_SETTINGS)))
    }

    private fun openControls(state: CapsuleState): Reduction = Reduction(
        state.copy(controlsOpen = true, isExpanded = true, focusedCard = CapsuleCardType.SLIDERS, miniEvent = null),
        listOf(Effect.RefreshControls)
    )

    /** Ring → vibrate → silent → ring. Silent needs DND access; without it, skip it and ask. */
    private fun cycleRinger(state: CapsuleState): Reduction {
        val next = when (state.sliders.ringerMode) {
            RingerMode.NORMAL -> RingerMode.VIBRATE
            RingerMode.VIBRATE -> RingerMode.SILENT
            RingerMode.SILENT -> RingerMode.NORMAL
        }
        if (next == RingerMode.SILENT && !state.sliders.canSilence) {
            return Reduction(
                state.copy(sliders = state.sliders.copy(ringerMode = RingerMode.NORMAL), isExpanded = false),
                listOf(Effect.Ringer(RingerMode.NORMAL), Effect.OpenPermission(Effect.PermissionScreen.DND_ACCESS))
            )
        }
        return Reduction(state.copy(sliders = state.sliders.copy(ringerMode = next)), listOf(Effect.Ringer(next)))
    }

    /** A control without its permission sends the user to grant it; the card closes meanwhile. */
    private fun askPermission(state: CapsuleState, screen: Effect.PermissionScreen): Reduction =
        Reduction(state.copy(isExpanded = false), listOf(Effect.OpenPermission(screen)))

    private fun playPause(state: CapsuleState): Reduction {
        val media = state.media ?: return Reduction(state)
        return Reduction(
            state.copy(media = media.copy(isPlaying = !media.isPlaying)),
            listOf(Effect.Media(Effect.MediaCommand.PLAY_PAUSE))
        )
    }

    private fun cycleCard(state: CapsuleState, step: Int): CapsuleState {
        val cards = state.activeCards
        val current = state.activeCard ?: return state
        val index = (cards.indexOf(current) + step).mod(cards.size)
        return state.copy(focusedCard = cards[index])
    }

    private fun reduceEvent(state: CapsuleState, event: SystemEvent): CapsuleState = when (event) {
        is SystemEvent.SettingsChanged -> state.copy(settings = event.settings, settingsLoaded = true)
        is SystemEvent.VisibilityChanged -> state.copy(visibility = event.visibility)
        is SystemEvent.MediaUpdated -> state.copy(media = event.media?.let { mediaWithPauseRule(state.media, it) })
        is SystemEvent.MediaPauseTimedOut -> state.copy(media = state.media?.let { media ->
            val matches = media.packageName == event.packageName && media.title == event.title
            if (matches && !media.isPlaying) media.copy(hiddenAfterPause = true) else media
        })
        is SystemEvent.SlidersUpdated -> state.copy(sliders = event.sliders)
        is SystemEvent.ChargingUpdated -> state.copy(charging = event.charging)
        is SystemEvent.NotificationPosted -> notificationPosted(state, event)
        is SystemEvent.NotificationRemoved -> state.withoutNotification(event.key)
        is SystemEvent.NotificationTimedOut -> {
            val beingRead = state.isExpanded && state.activeCard == CapsuleCardType.NOTIFICATION &&
                state.topNotification?.key == event.key
            if (beingRead || state.replyingTo == event.key) state else state.withoutNotification(event.key)
        }
        is SystemEvent.TimerUpdated -> state.copy(timer = event.timer)
        is SystemEvent.ProgressUpdated -> state.copy(
            progress = if (state.progress.any { it.key == event.item.key }) {
                state.progress.map { if (it.key == event.item.key) event.item else it }
            } else {
                state.progress + event.item
            }
        )
        is SystemEvent.ProgressRemoved -> state.copy(progress = state.progress.filter { it.key != event.key })
        is SystemEvent.MiniEventStarted ->
            if (event.event.type.category in state.settings.enabledEvents) state.copy(miniEvent = event.event) else state
        is SystemEvent.MiniEventFinished ->
            if (state.miniEvent?.id == event.id) state.copy(miniEvent = null) else state
        SystemEvent.ExpandedTimedOut -> state // handled in reduce(), it may emit effects
    }

    /**
     * A session first seen paused (e.g. a player left paused hours ago) starts hidden; a track
     * paused while we watched stays visible until the collapse delay fires (see CapsuleStore).
     */
    private fun mediaWithPauseRule(old: MediaState?, new: MediaState): MediaState {
        val hidden = when {
            new.isPlaying -> false
            new.isSameTrack(old) -> old!!.hiddenAfterPause
            else -> true
        }
        return new.copy(hiddenAfterPause = hidden)
    }

    private fun notificationPosted(state: CapsuleState, event: SystemEvent.NotificationPosted): CapsuleState {
        val incoming = event.notification
        if (!event.alert) {
            // Quiet update: refresh in place, never resurrect something the user already saw.
            return state.copy(notifications = state.notifications.map { if (it.key == incoming.key) incoming else it })
        }
        val queue = (listOf(incoming) + state.notifications.filter { it.key != incoming.key })
            .take(MAX_CAPSULE_NOTIFICATIONS)
        val autoExpand = state.settings.notificationDisplay == NotificationDisplay.EXPANDED &&
            state.settings.notificationsEnabled && state.isAllowedOnScreen
        return state.copy(
            notifications = queue,
            focusedCard = CapsuleCardType.NOTIFICATION,
            isExpanded = state.isExpanded || autoExpand,
            miniEvent = if (autoExpand) null else state.miniEvent
        )
    }

    /**
     * Closing an open notification card counts as having seen it (F-24 "until opened"): it
     * leaves the capsule, and with F-25 also the notification shade.
     */
    private fun collapse(state: CapsuleState): Reduction {
        val viewed = state.topNotification?.takeIf { state.isExpanded && state.activeCard == CapsuleCardType.NOTIFICATION }
        val collapsed = state.copy(isExpanded = false, replyingTo = null, controlsOpen = false)
        if (viewed == null) return Reduction(collapsed)
        val effects = if (state.settings.removeNotificationAfterViewing && viewed.clearable) {
            listOf(Effect.NotificationDismiss(viewed.key))
        } else {
            emptyList()
        }
        return Reduction(collapsed.withoutNotification(viewed.key), effects)
    }

    private fun CapsuleState.withoutNotification(key: String): CapsuleState = copy(
        notifications = notifications.filter { it.key != key },
        replyingTo = replyingTo.takeIf { it != key }
    )

    /** Invariants that hold after every action. */
    private fun normalize(state: CapsuleState): CapsuleState {
        val expandable = state.activeCards.isNotEmpty() && state.isAllowedOnScreen
        val expanded = state.isExpanded && expandable
        val replying = state.replyingTo.takeIf { expanded && state.activeCard == CapsuleCardType.NOTIFICATION }
        val controls = state.controlsOpen && expanded
        return if (expanded == state.isExpanded && replying == state.replyingTo && controls == state.controlsOpen) state
        else state.copy(isExpanded = expanded, replyingTo = replying, controlsOpen = controls)
    }
}
