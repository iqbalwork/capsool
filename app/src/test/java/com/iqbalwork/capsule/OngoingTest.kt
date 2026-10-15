package com.iqbalwork.capsule

import android.app.Notification
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.ProgressItem
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.model.formatClock
import com.iqbalwork.capsule.platform.notification.OngoingClassifier
import com.iqbalwork.capsule.platform.notification.OngoingFacts
import com.iqbalwork.capsule.platform.notification.OngoingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OngoingTest {

    private val plain = OngoingFacts(
        packageName = "app", isOngoing = true, isMediaStyle = false, category = null,
        progress = 0, progressMax = 0, indeterminate = false, showChronometer = false, chronometerCountDown = false
    )

    // --- Classifier ---

    @Test
    fun chronometerIsATimer() {
        assertEquals(OngoingKind.TIMER, OngoingClassifier.classify(plain.copy(showChronometer = true), knownTimer = false))
        // Paused: no chronometer, but the same ongoing notification stays a timer.
        assertEquals(OngoingKind.TIMER, OngoingClassifier.classify(plain, knownTimer = true))
        assertEquals(OngoingKind.NONE, OngoingClassifier.classify(plain, knownTimer = false))
        // A finished (dismissable) timer is no longer one.
        assertEquals(OngoingKind.NONE, OngoingClassifier.classify(plain.copy(isOngoing = false, showChronometer = true), false))
    }

    @Test
    fun progressBarsAreProgress() {
        assertEquals(OngoingKind.PROGRESS, OngoingClassifier.classify(plain.copy(progressMax = 100, progress = 30), false))
        assertEquals(OngoingKind.PROGRESS, OngoingClassifier.classify(plain.copy(indeterminate = true), false))
        assertEquals(0.3f, OngoingClassifier.fraction(plain.copy(progressMax = 100, progress = 30))!!, 0.001f)
        assertNull(OngoingClassifier.fraction(plain.copy(indeterminate = true, progressMax = 100)))
        assertTrue(OngoingClassifier.isFinished(plain.copy(progressMax = 100, progress = 100)))
        assertFalse(OngoingClassifier.isFinished(plain.copy(progressMax = 100, progress = 99)))
    }

    @Test
    fun callsNavigationAndMediaAreExcluded() {
        val timerLike = plain.copy(showChronometer = true, progressMax = 100)
        for (category in listOf(Notification.CATEGORY_CALL, Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_TRANSPORT)) {
            assertEquals(OngoingKind.NONE, OngoingClassifier.classify(timerLike.copy(category = category), false))
        }
        assertEquals(OngoingKind.NONE, OngoingClassifier.classify(timerLike.copy(isMediaStyle = true), false))
    }

    // --- Timer display ---

    private fun timer(countdown: Boolean, base: Long, running: Boolean = true, text: String = "") = TimerState(
        key = "t", packageName = "clock", appName = "Clock", label = "Timer",
        isCountdown = countdown, baseTimeMs = base, isRunning = running, staticText = text
    )

    @Test
    fun countdownAndStopwatchSeconds() {
        assertEquals(120, timer(countdown = true, base = 120_000).displaySeconds(0))
        assertEquals(1, timer(countdown = true, base = 120_000).displaySeconds(119_500))
        assertEquals(0, timer(countdown = true, base = 120_000).displaySeconds(200_000))
        assertEquals(65, timer(countdown = false, base = 1_000).displaySeconds(66_400))
        assertEquals("1:05", formatClock(65))
        assertEquals("1:01:01", formatClock(3661))
    }

    // --- Store ---

    private val loaded = CapsuleState(settingsLoaded = true)
    private fun item(key: String, fraction: Float?) = ProgressItem(key, "app", "App", "file-$key", fraction, color = 0xFF000000)
    private fun reduce(state: CapsuleState, vararg actions: Action) = actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    @Test
    fun progressItemsAreAddedUpdatedAndRemovedInPlace() {
        val state = reduce(
            loaded,
            SystemEvent.ProgressUpdated(item("a", 0.1f)),
            SystemEvent.ProgressUpdated(item("b", null)),
            SystemEvent.ProgressUpdated(item("a", 0.6f))
        )
        assertEquals(listOf("a", "b"), state.progress.map { it.key })
        assertEquals(0.6f, state.progress[0].fraction!!, 0f)
        assertEquals(listOf("b"), reduce(state, SystemEvent.ProgressRemoved("a")).progress.map { it.key })
    }

    @Test
    fun timerOutranksMediaAndProgressOutranksCharging() {
        val state = reduce(
            loaded,
            SystemEvent.MediaUpdated(MediaState(packageName = "m", title = "t", isPlaying = true)),
            SystemEvent.ProgressUpdated(item("a", 0.5f)),
            SystemEvent.TimerUpdated(timer(true, 1_000))
        )
        assertEquals(listOf(CapsuleCardType.TIMER, CapsuleCardType.MEDIA, CapsuleCardType.PROGRESS), state.activeCards)
        val off = state.copy(settings = Settings(timerEnabled = false, progressEnabled = false))
        assertEquals(listOf(CapsuleCardType.MEDIA), off.activeCards)
    }

    @Test
    fun timerButtonsRunTheClockAppsAction() {
        val state = reduce(loaded, SystemEvent.TimerUpdated(timer(true, 1_000)))
        val reduction = CapsuleReducer.reduce(state, UserIntent.TimerAction("t", 1))
        assertEquals(listOf(Effect.NotificationAction("t", 1)), reduction.effects)
        assertEquals(state.timer, reduction.state.timer)
    }
}
