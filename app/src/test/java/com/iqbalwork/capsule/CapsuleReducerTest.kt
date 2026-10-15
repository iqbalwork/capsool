package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.core.store.VisibilityContext
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapsuleReducerTest {

    private val loaded = CapsuleState(settingsLoaded = true)
    private val media = MediaState(title = "Song", artist = "Artist", isPlaying = true, durationMs = 100_000L)
    private val timer = TimerState(
        key = "t", packageName = "clock", appName = "Clock", label = "Timer",
        isCountdown = true, baseTimeMs = 60_000L, isRunning = true
    )

    private fun reduce(state: CapsuleState, vararg actions: com.iqbalwork.capsule.core.store.Action): CapsuleState =
        actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    @Test
    fun hiddenUntilSettingsLoaded() {
        assertEquals(CapsuleMode.HIDDEN, CapsuleState().mode)
        assertEquals(CapsuleMode.IDLE, loaded.mode)
    }

    @Test
    fun idlePillHiddenWhenAlwaysShowIsOff() {
        val state = loaded.copy(settings = Settings(alwaysShowPill = false))
        assertEquals(CapsuleMode.HIDDEN, state.mode)
        assertEquals(CapsuleMode.COMPACT, reduce(state, SystemEvent.MediaUpdated(media)).mode)
    }

    @Test
    fun visibilityRulesHideCapsule() {
        val fullscreen = loaded.copy(visibility = VisibilityContext(fullscreen = true))
        assertEquals(CapsuleMode.HIDDEN, fullscreen.mode)
        assertEquals(CapsuleMode.IDLE, fullscreen.copy(settings = Settings(hideInFullscreen = false)).mode)
        assertEquals(CapsuleMode.HIDDEN, loaded.copy(visibility = VisibilityContext(imeVisible = true)).mode)
        assertEquals(CapsuleMode.HIDDEN, loaded.copy(visibility = VisibilityContext(orientation = Orientation.LANDSCAPE)).mode)
        assertEquals(CapsuleMode.HIDDEN, loaded.copy(visibility = VisibilityContext(screenOn = false)).mode)
    }

    @Test
    fun tapWithoutActivityOpensControls() {
        val state = reduce(loaded, UserIntent.Gesture(GestureType.TAP))
        assertEquals(CapsuleMode.EXPANDED, state.mode)
        assertEquals(CapsuleCardType.SLIDERS, state.activeCard)
    }

    @Test
    fun tapTogglesExpandWhenThereIsActivity() {
        val withMedia = reduce(loaded, SystemEvent.MediaUpdated(media))
        val expanded = reduce(withMedia, UserIntent.Gesture(GestureType.TAP))
        assertEquals(CapsuleMode.EXPANDED, expanded.mode)
        assertEquals(CapsuleMode.COMPACT, reduce(expanded, UserIntent.Gesture(GestureType.TAP)).mode)
    }

    @Test
    fun collapsesWhenLastActivityEnds() {
        val expanded = reduce(loaded, SystemEvent.MediaUpdated(media), UserIntent.Gesture(GestureType.TAP))
        val ended = reduce(expanded, SystemEvent.MediaUpdated(null))
        assertFalse(ended.isExpanded)
    }

    @Test
    fun collapsesWhenHiddenByVisibilityRule() {
        val expanded = reduce(loaded, SystemEvent.MediaUpdated(media), UserIntent.Gesture(GestureType.TAP))
        val keyboard = reduce(expanded, SystemEvent.VisibilityChanged(VisibilityContext(imeVisible = true)))
        assertFalse(keyboard.isExpanded)
    }

    @Test
    fun cardsAreOrderedByPriorityAndSwipeCycles() {
        val state = reduce(
            loaded,
            SystemEvent.MediaUpdated(media),
            SystemEvent.ChargingUpdated(ChargingState(isCharging = true, chargePercentage = 50)),
            SystemEvent.TimerUpdated(timer)
        )
        assertEquals(listOf(CapsuleCardType.TIMER, CapsuleCardType.MEDIA, CapsuleCardType.CHARGING), state.activeCards)
        assertEquals(CapsuleCardType.TIMER, state.activeCard)

        val next = reduce(state, UserIntent.Gesture(GestureType.SWIPE_LEFT))
        assertEquals(CapsuleCardType.MEDIA, next.activeCard)
        val wrapped = reduce(state, UserIntent.Gesture(GestureType.SWIPE_RIGHT))
        assertEquals(CapsuleCardType.CHARGING, wrapped.activeCard)
    }

    @Test
    fun playPauseEmitsEffectAndUpdatesOptimistically() {
        val withMedia = reduce(loaded, SystemEvent.MediaUpdated(media))
        val reduction = CapsuleReducer.reduce(withMedia, UserIntent.Gesture(GestureType.DOUBLE_TAP))
        assertFalse(reduction.state.media!!.isPlaying)
        assertEquals(listOf(Effect.Media(Effect.MediaCommand.PLAY_PAUSE)), reduction.effects)
    }

    @Test
    fun playPauseWithoutMediaDoesNothing() {
        val reduction = CapsuleReducer.reduce(loaded, UserIntent.Gesture(GestureType.DOUBLE_TAP))
        assertTrue(reduction.effects.isEmpty())
    }

    @Test
    fun gestureMappingComesFromSettings() {
        val settings = Settings(gestures = mapOf(GestureType.LONG_PRESS to GestureAction.OPEN_QUICK_SETTINGS))
        val reduction = CapsuleReducer.reduce(loaded.copy(settings = settings), UserIntent.Gesture(GestureType.LONG_PRESS))
        assertEquals(listOf(Effect.Global(Effect.GlobalAction.QUICK_SETTINGS)), reduction.effects)

        val none = Settings(gestures = mapOf(GestureType.LONG_PRESS to GestureAction.NONE))
        assertTrue(CapsuleReducer.reduce(loaded.copy(settings = none), UserIntent.Gesture(GestureType.LONG_PRESS)).effects.isEmpty())
    }

    @Test
    fun miniEventFinishesOnlyForMatchingId() {
        val event = MiniEvent(id = 1L, type = MiniEventType.CHARGER_CONNECTED, title = "Charging")
        val shown = reduce(loaded, SystemEvent.MiniEventStarted(event))
        assertEquals(CapsuleMode.MINI_EVENT, shown.mode)
        assertEquals(event, reduce(shown, SystemEvent.MiniEventFinished(2L)).miniEvent)
        assertEquals(null, reduce(shown, SystemEvent.MiniEventFinished(1L)).miniEvent)
    }

    @Test
    fun seekIsClampedToDuration() {
        val withMedia = reduce(loaded, SystemEvent.MediaUpdated(media))
        val reduction = CapsuleReducer.reduce(withMedia, UserIntent.MediaSeek(500_000L))
        assertEquals(100_000L, reduction.state.media!!.playbackPositionMs)
        assertEquals(listOf(Effect.MediaSeek(100_000L)), reduction.effects)
    }

    @Test
    fun newNotificationTakesFocus() {
        val state = reduce(loaded, SystemEvent.MediaUpdated(media), UserIntent.SelectCard(CapsuleCardType.MEDIA))
        val notified = reduce(state, SystemEvent.NotificationPosted(NotificationFixtures.notification("1")))
        assertEquals(CapsuleCardType.NOTIFICATION, notified.activeCard)
    }
}
