package com.iqbalwork.capsule

import android.app.Notification
import android.app.NotificationManager
import com.iqbalwork.capsule.NotificationFixtures.notification
import com.iqbalwork.capsule.core.settings.NotificationDisplay
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.core.store.VisibilityContext
import com.iqbalwork.capsule.platform.notification.NotificationDecision
import com.iqbalwork.capsule.platform.notification.NotificationFacts
import com.iqbalwork.capsule.platform.notification.NotificationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {

    private val loaded = CapsuleState(settingsLoaded = true)

    private fun reduce(state: CapsuleState, vararg actions: Action) =
        actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    // --- Filter ---

    private val filter = NotificationFilter(ownPackage = "com.iqbalwork.capsule")
    private val chat = NotificationFacts(
        key = "k", packageName = "com.chat", isOngoing = false, isGroupSummary = false, isMediaStyle = false,
        category = Notification.CATEGORY_MESSAGE, importance = NotificationManager.IMPORTANCE_DEFAULT,
        hasContent = true, onlyAlertOnce = false
    )

    @Test
    fun filterShowsOrdinaryMessages() {
        assertEquals(NotificationDecision.SHOW, filter.decide(chat, emptySet(), alreadyKnown = false))
    }

    @Test
    fun filterIgnoresNoise() {
        val ignored = listOf(
            chat.copy(packageName = "com.iqbalwork.capsule"),
            chat.copy(packageName = "com.android.systemui"),
            chat.copy(packageName = "android"),
            chat.copy(isOngoing = true),
            chat.copy(isGroupSummary = true),
            chat.copy(isMediaStyle = true),
            chat.copy(category = Notification.CATEGORY_TRANSPORT),
            chat.copy(category = Notification.CATEGORY_CALL),
            chat.copy(importance = NotificationManager.IMPORTANCE_LOW),
            chat.copy(hasContent = false)
        )
        ignored.forEach { assertEquals(it.toString(), NotificationDecision.IGNORE, filter.decide(it, emptySet(), false)) }
        assertEquals(NotificationDecision.IGNORE, filter.decide(chat, setOf("com.chat"), false))
    }

    @Test
    fun filterTreatsAlertOnceUpdatesAsQuiet() {
        val update = chat.copy(onlyAlertOnce = true)
        assertEquals(NotificationDecision.UPDATE_IN_PLACE, filter.decide(update, emptySet(), alreadyKnown = true))
        assertEquals(NotificationDecision.SHOW, filter.decide(update, emptySet(), alreadyKnown = false))
    }

    @Test
    fun debugBuildsMayShowOwnNotifications() {
        val debugFilter = NotificationFilter(ownPackage = "com.iqbalwork.capsule", ignoreOwnPackage = false)
        assertEquals(NotificationDecision.SHOW, debugFilter.decide(chat.copy(packageName = "com.iqbalwork.capsule"), emptySet(), false))
    }

    // --- Queue ---

    @Test
    fun queueKeepsNewestFirstAndCapsAtFive() {
        val state = reduce(loaded, *(1..7).map { SystemEvent.NotificationPosted(notification("$it")) }.toTypedArray())
        assertEquals(listOf("7", "6", "5", "4", "3"), state.notifications.map { it.key })
        val reposted = reduce(state, SystemEvent.NotificationPosted(notification("5")))
        assertEquals(listOf("5", "7", "6", "4", "3"), reposted.notifications.map { it.key })
    }

    @Test
    fun quietUpdateRefreshesInPlaceButNeverAdds() {
        val state = reduce(loaded, SystemEvent.NotificationPosted(notification("1")), SystemEvent.NotificationPosted(notification("2")))
        val updated = reduce(state, SystemEvent.NotificationPosted(notification("1").copy(text = "edited"), alert = false))
        assertEquals(listOf("2", "1"), updated.notifications.map { it.key })
        assertEquals("edited", updated.notifications[1].text)
        val unknown = reduce(state, SystemEvent.NotificationPosted(notification("9"), alert = false))
        assertEquals(listOf("2", "1"), unknown.notifications.map { it.key })
    }

    @Test
    fun removedFromShadeLeavesCapsule() {
        val state = reduce(loaded, SystemEvent.NotificationPosted(notification("1")), SystemEvent.NotificationRemoved("1"))
        assertTrue(state.notifications.isEmpty())
    }

    @Test
    fun expandedDisplayOpensTheCard() {
        val settings = Settings(notificationDisplay = NotificationDisplay.EXPANDED)
        val state = reduce(loaded.copy(settings = settings), SystemEvent.NotificationPosted(notification("1")))
        assertEquals(CapsuleMode.EXPANDED, state.mode)
        assertEquals(CapsuleMode.COMPACT, reduce(loaded, SystemEvent.NotificationPosted(notification("1"))).mode)
    }

    @Test
    fun disabledNotificationsNeverShow() {
        val state = reduce(loaded.copy(settings = Settings(notificationsEnabled = false)), SystemEvent.NotificationPosted(notification("1")))
        assertTrue(state.activeCards.isEmpty())
    }

    // --- Timeouts and viewing ---

    @Test
    fun timeoutRemovesUnlessBeingRead() {
        val posted = reduce(loaded, SystemEvent.NotificationPosted(notification("1")))
        assertTrue(reduce(posted, SystemEvent.NotificationTimedOut("1")).notifications.isEmpty())
        val reading = posted.copy(isExpanded = true)
        assertEquals(1, reduce(reading, SystemEvent.NotificationTimedOut("1")).notifications.size)
    }

    @Test
    fun collapsingAnOpenNotificationMarksItViewed() {
        val open = reduce(loaded, SystemEvent.NotificationPosted(notification("1"))).copy(isExpanded = true)
        val reduction = CapsuleReducer.reduce(open, UserIntent.Collapse)
        assertTrue(reduction.state.notifications.isEmpty())
        assertTrue(reduction.effects.isEmpty())

        val removing = open.copy(settings = Settings(removeNotificationAfterViewing = true))
        assertEquals(listOf(Effect.NotificationDismiss("1")), CapsuleReducer.reduce(removing, UserIntent.Collapse).effects)
        val sticky = reduce(loaded, SystemEvent.NotificationPosted(notification("2", clearable = false)))
            .copy(isExpanded = true, settings = Settings(removeNotificationAfterViewing = true))
        assertTrue(CapsuleReducer.reduce(sticky, UserIntent.Collapse).effects.isEmpty())
    }

    @Test
    fun expandedTimeoutAlsoCountsAsViewed() {
        val open = reduce(loaded, SystemEvent.NotificationPosted(notification("1"))).copy(isExpanded = true)
        assertTrue(reduce(open, SystemEvent.ExpandedTimedOut).notifications.isEmpty())
    }

    // --- Actions ---

    @Test
    fun actionsRemoveAndEmitEffects() {
        val state = reduce(loaded, SystemEvent.NotificationPosted(notification("1")))
        assertEquals(listOf(Effect.NotificationMarkRead("1")), CapsuleReducer.reduce(state, UserIntent.NotificationMarkRead("1")).effects)
        assertEquals(listOf(Effect.NotificationDismiss("1")), CapsuleReducer.reduce(state, UserIntent.NotificationDismiss("1")).effects)
        val open = CapsuleReducer.reduce(state.copy(isExpanded = true), UserIntent.NotificationOpen("1"))
        assertEquals(listOf(Effect.NotificationOpen("1")), open.effects)
        assertFalse(open.state.isExpanded)
    }

    @Test
    fun replyFlow() {
        val open = reduce(loaded, SystemEvent.NotificationPosted(notification("1"))).copy(isExpanded = true)
        val replying = reduce(open, UserIntent.NotificationStartReply("1"))
        assertEquals("1", replying.replyingTo)

        // Our own reply field opens the keyboard; that must not hide the capsule.
        val withKeyboard = reduce(replying, SystemEvent.VisibilityChanged(VisibilityContext(imeVisible = true)))
        assertEquals(CapsuleMode.EXPANDED, withKeyboard.mode)

        val sent = CapsuleReducer.reduce(withKeyboard, UserIntent.NotificationReply("1", "On my way"))
        assertEquals(listOf(Effect.NotificationReply("1", "On my way")), sent.effects)
        assertNull(sent.state.replyingTo)
        assertFalse(sent.state.isExpanded)
    }

    @Test
    fun replyNotAllowedWhenLockedOrUnsupported() {
        val open = reduce(loaded, SystemEvent.NotificationPosted(notification("1"))).copy(isExpanded = true)
        val locked = open.copy(visibility = VisibilityContext(locked = true))
        assertNull(reduce(locked, UserIntent.NotificationStartReply("1")).replyingTo)
        val noReply = reduce(loaded, SystemEvent.NotificationPosted(notification("2", canReply = false))).copy(isExpanded = true)
        assertNull(reduce(noReply, UserIntent.NotificationStartReply("2")).replyingTo)
    }

    @Test
    fun replyStateClearsWhenCardCloses() {
        val replying = reduce(loaded, SystemEvent.NotificationPosted(notification("1")))
            .copy(isExpanded = true, replyingTo = "1")
        assertNull(reduce(replying, UserIntent.Collapse).replyingTo)
        assertNull(reduce(replying, SystemEvent.NotificationRemoved("1")).replyingTo)
    }

    // --- Privacy ---

    @Test
    fun textHiddenOnLockScreenUnlessPublicOrAllowed() {
        val private = notification("1")
        val public = notification("2", isPublic = true)
        val locked = loaded.copy(visibility = VisibilityContext(locked = true))
        assertFalse(locked.showsText(private))
        assertTrue(locked.showsText(public))
        assertTrue(locked.copy(settings = Settings(hideNotificationTextWhenLocked = false)).showsText(private))
        assertTrue(loaded.showsText(private))
    }
}
