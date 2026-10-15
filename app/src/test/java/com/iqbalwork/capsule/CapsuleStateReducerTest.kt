package com.iqbalwork.capsule

import com.iqbalwork.capsule.engine.stack.CardStackManager
import com.iqbalwork.capsule.engine.state.CapsuleState
import com.iqbalwork.capsule.engine.state.CapsuleStateReducer
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.VolumeSliderStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CapsuleStateReducerTest {

    private lateinit var stackManager: CardStackManager
    private lateinit var initialState: CapsuleState

    @Before
    fun setUp() {
        stackManager = CardStackManager()
        initialState = CapsuleState(
            availableCards = stackManager.getCards(),
            activeCard = stackManager.getActiveCard(),
            activeCardIndex = stackManager.getActiveIndex()
        )
    }

    @Test
    fun testTapTogglesExpansion() {
        assertFalse(initialState.isExpanded)

        val expanded = CapsuleStateReducer.reduce(initialState, UserAction.Tap, stackManager)
        assertTrue(expanded.isExpanded)

        val collapsed = CapsuleStateReducer.reduce(expanded, UserAction.Tap, stackManager)
        assertFalse(collapsed.isExpanded)
    }

    @Test
    fun testDoubleTapTogglesMediaPlayback() {
        val playingState = initialState.copy(
            activeCard = CapsuleCardType.MEDIA,
            mediaState = initialState.mediaState.copy(isPlaying = true)
        )
        val pausedState = CapsuleStateReducer.reduce(playingState, UserAction.DoubleTap, stackManager)
        assertFalse(pausedState.mediaState.isPlaying)

        val resumedState = CapsuleStateReducer.reduce(pausedState, UserAction.DoubleTap, stackManager)
        assertTrue(resumedState.mediaState.isPlaying)
    }

    @Test
    fun testLongPressExpandsFullCard() {
        assertFalse(initialState.isExpanded)
        val result = CapsuleStateReducer.reduce(initialState, UserAction.LongPress, stackManager)
        assertTrue(result.isExpanded)
    }

    @Test
    fun testSwipeLeftTransitionsToNextCard() {
        assertEquals(CapsuleCardType.MEDIA, initialState.activeCard)

        val nextState = CapsuleStateReducer.reduce(initialState, UserAction.SwipeLeft, stackManager)
        assertEquals(CapsuleCardType.SLIDERS, nextState.activeCard)
        assertEquals(1, nextState.activeCardIndex)
    }

    @Test
    fun testSwipeRightTransitionsToPreviousCard() {
        assertEquals(CapsuleCardType.MEDIA, initialState.activeCard)

        val prevState = CapsuleStateReducer.reduce(initialState, UserAction.SwipeRight, stackManager)
        assertEquals(CapsuleCardType.SHORTCUTS, prevState.activeCard)
        assertEquals(5, prevState.activeCardIndex)
    }

    @Test
    fun testVolumeAndStyleControls() {
        val volState = CapsuleStateReducer.reduce(initialState, UserAction.SetVolume(0.85f), stackManager)
        assertEquals(0.85f, volState.sliderState.volumeLevel, 0.001f)

        // Cycle Volume Style: A15 -> A16 -> OneUI
        assertEquals(VolumeSliderStyle.ANDROID_15, volState.sliderState.volumeStyle)
        val a16State = CapsuleStateReducer.reduce(volState, UserAction.CycleVolumeStyle, stackManager)
        assertEquals(VolumeSliderStyle.ANDROID_16, a16State.sliderState.volumeStyle)

        val oneUiState = CapsuleStateReducer.reduce(a16State, UserAction.CycleVolumeStyle, stackManager)
        assertEquals(VolumeSliderStyle.SAMSUNG_ONE_UI, oneUiState.sliderState.volumeStyle)

        // Cycle Ringer Mode: Normal -> Vibrate -> Silent
        assertEquals(RingerMode.NORMAL, oneUiState.sliderState.ringerMode)
        val vibState = CapsuleStateReducer.reduce(oneUiState, UserAction.CycleRingerMode, stackManager)
        assertEquals(RingerMode.VIBRATE, vibState.sliderState.ringerMode)
    }

    @Test
    fun testTorchAndBrightnessControls() {
        val brightState = CapsuleStateReducer.reduce(initialState, UserAction.SetBrightness(0.92f), stackManager)
        assertEquals(0.92f, brightState.sliderState.brightnessLevel, 0.001f)

        val torchOn = CapsuleStateReducer.reduce(brightState, UserAction.ToggleTorch, stackManager)
        assertTrue(torchOn.sliderState.isTorchOn)

        val torchOff = CapsuleStateReducer.reduce(torchOn, UserAction.ToggleTorch, stackManager)
        assertFalse(torchOff.sliderState.isTorchOn)
    }

    @Test
    fun testTimerControls() {
        val initialRemaining = initialState.timerState.remainingSeconds
        val addedState = CapsuleStateReducer.reduce(initialState, UserAction.TimerAddMinute, stackManager)
        assertEquals(initialRemaining + 60, addedState.timerState.remainingSeconds)

        val pausedState = CapsuleStateReducer.reduce(addedState, UserAction.TimerTogglePause, stackManager)
        assertFalse(pausedState.timerState.isRunning)

        val resetState = CapsuleStateReducer.reduce(pausedState, UserAction.TimerReset, stackManager)
        assertEquals(resetState.timerState.totalSeconds, resetState.timerState.remainingSeconds)
        assertFalse(resetState.timerState.isRunning)
    }

    @Test
    fun testNotificationActions() {
        val stateWithNotif = initialState.copy(notificationState = NotificationState())
        assertNotNull(stateWithNotif.notificationState)

        val dismissed = CapsuleStateReducer.reduce(
            stateWithNotif,
            UserAction.DismissNotification("demo_1"),
            stackManager
        )
        assertNull(dismissed.notificationState)
    }

    @Test
    fun testMiniEventLifecycle() {
        val event = MiniEvent(MiniEventType.TWS_CONNECTED, "AirPods Pro", "• 90%")
        val stateWithEvent = CapsuleStateReducer.reduce(
            initialState,
            UserAction.TriggerMiniEvent(event),
            stackManager
        )
        assertEquals(event, stateWithEvent.activeMiniEvent)

        val dismissed = CapsuleStateReducer.reduce(
            stateWithEvent,
            UserAction.DismissMiniEvent,
            stackManager
        )
        assertNull(dismissed.activeMiniEvent)
    }
}
