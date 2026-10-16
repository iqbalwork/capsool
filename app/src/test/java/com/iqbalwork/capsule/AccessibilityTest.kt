package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.LockScreenBehavior
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.core.store.VisibilityContext
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.ui.overlay.capsuleSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen-reader support: fixed-meaning actions and what the capsule says about itself. */
class AccessibilityTest {

    private val timer = TimerState(
        key = "t", packageName = "clock", appName = "Clock", label = "Tea",
        isCountdown = true, baseTimeMs = 60_000L, isRunning = true
    )
    private val media = MediaState(packageName = "music", title = "Song", artist = "Band", isPlaying = true)

    private fun state(
        expanded: Boolean = false,
        locked: Boolean = false,
        behavior: LockScreenBehavior = LockScreenBehavior.SHOW,
        withMedia: Boolean = false,
        withTimer: Boolean = false,
        notifications: List<com.iqbalwork.capsule.model.CapsuleNotification> = emptyList(),
        miniEvent: MiniEvent? = null
    ) = CapsuleState(
        settingsLoaded = true,
        settings = Settings(lockScreenBehavior = behavior),
        visibility = VisibilityContext(locked = locked),
        isExpanded = expanded,
        media = media.takeIf { withMedia },
        timer = timer.takeIf { withTimer },
        notifications = notifications,
        miniEvent = miniEvent
    )

    private fun res(text: UiText) = text as UiText.Res

    // --- Perform: the same behaviour as the gestures, but with a fixed meaning ---

    @Test
    fun performToggleExpandOpensAndClosesTheCapsule() {
        val opened = CapsuleReducer.reduce(state(withTimer = true), UserIntent.Perform(GestureAction.TOGGLE_EXPAND)).state
        assertTrue(opened.isExpanded)
        val closed = CapsuleReducer.reduce(opened, UserIntent.Perform(GestureAction.TOGGLE_EXPAND)).state
        assertFalse(closed.isExpanded)
    }

    @Test
    fun performIgnoresTheUsersGestureBindings() {
        // A user who rebound tap to "do nothing" must still be able to open the capsule by accessibility action.
        val rebound = state(withTimer = true).copy(
            settings = Settings(gesturesActive = mapOf(GestureType.TAP to GestureBinding.None))
        )
        assertTrue(CapsuleReducer.reduce(rebound, UserIntent.Perform(GestureAction.TOGGLE_EXPAND)).state.isExpanded)
    }

    @Test
    fun performNextAndPreviousCardMoveBetweenPages() {
        val two = state(expanded = true, withTimer = true, withMedia = true)
        val first = two.activePage
        val next = CapsuleReducer.reduce(two, UserIntent.Perform(GestureAction.NEXT_CARD)).state
        assertTrue(next.activePage != first)
        val back = CapsuleReducer.reduce(next, UserIntent.Perform(GestureAction.PREVIOUS_CARD)).state
        assertEquals(first, back.activePage)
    }

    // --- Summary ---

    @Test
    fun idleCapsuleHasAGenericName() {
        assertEquals(R.string.a11y_summary_idle, res(capsuleSummary(state())).id)
    }

    @Test
    fun collapsedCapsuleDescribesTheActiveCard() {
        val summary = res(capsuleSummary(state(withMedia = true)))
        assertEquals(R.string.a11y_summary_collapsed, summary.id)
        val detail = summary.args.single() as UiText.Res
        assertEquals(R.string.a11y_card_media_playing, detail.id)
        assertEquals(listOf<Any>("Song", "Band"), detail.args)
    }

    @Test
    fun expandedCapsuleSaysItIsOpen() {
        assertEquals(R.string.a11y_summary_expanded, res(capsuleSummary(state(expanded = true, withTimer = true))).id)
    }

    @Test
    fun miniEventIsAnnouncedWhileCollapsedButNotOnceOpen() {
        val event = MiniEvent(1L, MiniEventType.DEVICE_UNLOCKED, "Welcome back")
        assertEquals(R.string.a11y_summary_event, res(capsuleSummary(state(miniEvent = event))).id)
        assertEquals(
            R.string.a11y_summary_expanded,
            res(capsuleSummary(state(expanded = true, withTimer = true, miniEvent = event))).id
        )
    }

    @Test
    fun notificationTextIsReadOutOnlyWhenTheCardWouldShowIt() {
        val n = NotificationFixtures.notification("a")
        val shown = res(capsuleSummary(state(notifications = listOf(n)))).args.single() as UiText.Res
        assertEquals(R.string.a11y_card_notification_text, shown.id)

        val hidden = res(
            capsuleSummary(
                state(notifications = listOf(n), locked = true, behavior = LockScreenBehavior.SHOW_WITHOUT_CONTENT)
            )
        ).args.single() as UiText.Res
        assertEquals(R.string.a11y_card_notification, hidden.id)
        assertEquals(listOf<Any>("Chat"), hidden.args)
    }
}
