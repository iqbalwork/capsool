package com.iqbalwork.capsule.core.store

import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.VolumeSliderStyle

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
            is SystemEvent -> Reduction(reduceEvent(state, action))
        }
        return reduction.copy(state = normalize(reduction.state))
    }

    private fun reduceIntent(state: CapsuleState, intent: UserIntent): Reduction = when (intent) {
        is UserIntent.Gesture -> reduceGesture(state, state.settings.gestureAction(intent.type))
        UserIntent.SwipeUp, UserIntent.Collapse -> Reduction(state.copy(isExpanded = false))
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
        UserIntent.CycleVolumeStyle -> {
            val styles = VolumeSliderStyle.entries
            val next = styles[(styles.indexOf(state.sliders.volumeStyle) + 1) % styles.size]
            Reduction(state.copy(sliders = state.sliders.copy(volumeStyle = next)))
        }
        UserIntent.CycleRinger -> {
            val modes = RingerMode.entries
            val next = modes[(modes.indexOf(state.sliders.ringerMode) + 1) % modes.size]
            Reduction(state.copy(sliders = state.sliders.copy(ringerMode = next)), listOf(Effect.Ringer(next)))
        }
        is UserIntent.SetBrightness -> {
            val level = intent.level.coerceIn(0f, 1f)
            Reduction(state.copy(sliders = state.sliders.copy(brightnessLevel = level)), listOf(Effect.Brightness(level)))
        }
        UserIntent.ToggleTorch -> {
            val on = !state.sliders.isTorchOn
            Reduction(state.copy(sliders = state.sliders.copy(isTorchOn = on)), listOf(Effect.Torch(on, null)))
        }
        is UserIntent.SetTorchLevel -> {
            val level = intent.level.coerceIn(0f, 1f)
            val on = level > 0.05f
            Reduction(
                state.copy(sliders = state.sliders.copy(torchStrengthLevel = level, isTorchOn = on)),
                listOf(Effect.Torch(on, level))
            )
        }

        is UserIntent.NotificationReply ->
            Reduction(state.copy(notification = null), listOf(Effect.NotificationReply(intent.id, intent.text)))
        is UserIntent.NotificationDismiss ->
            Reduction(state.copy(notification = null), listOf(Effect.NotificationDismiss(intent.id)))

        // Timer controls are wired to the clock app's notification actions in M5.
        UserIntent.TimerAddMinute -> Reduction(state.copy(timer = state.timer?.let {
            it.copy(remainingSeconds = it.remainingSeconds + 60, totalSeconds = it.totalSeconds + 60)
        }))
        UserIntent.TimerTogglePause -> Reduction(state.copy(timer = state.timer?.let { it.copy(isRunning = !it.isRunning) }))
        UserIntent.TimerReset -> Reduction(state.copy(timer = state.timer?.let {
            it.copy(remainingSeconds = it.totalSeconds, isRunning = false)
        }))

        is UserIntent.LaunchShortcut -> Reduction(state.copy(isExpanded = false), listOf(Effect.LaunchShortcut(intent.id)))
    }

    private fun reduceGesture(state: CapsuleState, action: GestureAction): Reduction = when (action) {
        GestureAction.NONE -> Reduction(state)
        GestureAction.TOGGLE_EXPAND ->
            if (state.activeCards.isEmpty()) Reduction(state)
            else Reduction(state.copy(isExpanded = !state.isExpanded, miniEvent = null))
        GestureAction.NEXT_CARD -> Reduction(cycleCard(state, step = 1))
        GestureAction.PREVIOUS_CARD -> Reduction(cycleCard(state, step = -1))
        GestureAction.MEDIA_PLAY_PAUSE -> playPause(state)
        GestureAction.OPEN_NOTIFICATIONS ->
            Reduction(state.copy(isExpanded = false), listOf(Effect.Global(Effect.GlobalAction.NOTIFICATIONS)))
        GestureAction.OPEN_QUICK_SETTINGS ->
            Reduction(state.copy(isExpanded = false), listOf(Effect.Global(Effect.GlobalAction.QUICK_SETTINGS)))
    }

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
        is SystemEvent.NotificationUpdated -> state.copy(
            notification = event.notification,
            focusedCard = if (event.notification != null) CapsuleCardType.NOTIFICATION else state.focusedCard
        )
        is SystemEvent.TimerUpdated -> state.copy(timer = event.timer)
        is SystemEvent.MiniEventStarted -> state.copy(miniEvent = event.event)
        is SystemEvent.MiniEventFinished ->
            if (state.miniEvent?.id == event.id) state.copy(miniEvent = null) else state
        SystemEvent.ExpandedTimedOut -> state.copy(isExpanded = false)
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

    /** Invariants that hold after every action. */
    private fun normalize(state: CapsuleState): CapsuleState =
        if (state.isExpanded && (state.activeCards.isEmpty() || !state.isAllowedOnScreen)) {
            state.copy(isExpanded = false)
        } else {
            state
        }
}
