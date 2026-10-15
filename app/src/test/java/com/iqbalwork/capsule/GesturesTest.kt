package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.platform.shortcuts.ShortcutLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturesTest {

    private val loaded = CapsuleState(settingsLoaded = true)
    private val media = MediaState(packageName = "p", title = "Song", isPlaying = true)
    private fun reduce(state: CapsuleState, vararg actions: Action) = actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }
    private fun gesture(state: CapsuleState, type: GestureType) = CapsuleReducer.reduce(state, UserIntent.Gesture(type))

    // --- Encoding ---

    @Test
    fun bindingsRoundTrip() {
        val withTarget = GestureBinding(GestureAction.OPEN_APP, "com.example:weird")
        assertEquals(withTarget, GestureBinding.decode(withTarget.encode()))
        assertEquals(GestureBinding(GestureAction.SCREENSHOT), GestureBinding.decode("SCREENSHOT"))
        assertNull(GestureBinding.decode("NOT_AN_ACTION"))
        assertNull(GestureBinding.decode(null))
    }

    @Test
    fun shortcutsRoundTripAndSkipBrokenRecords() {
        val list = listOf(
            Shortcut("a", ShortcutKind.APP, "Maps, Navigation", "com.maps"),
            Shortcut("b", ShortcutKind.CONTACT, "Ibu | Rumah", "+62 812", ContactAction.WHATSAPP)
        )
        assertEquals(list, Shortcut.decodeAll(Shortcut.encodeAll(list)))
        assertTrue(Shortcut.decodeAll("").isEmpty())
        assertTrue(Shortcut.decodeAll("garbage").isEmpty())
    }

    @Test
    fun whatsAppLinkUsesDigitsOnly() {
        assertEquals("628123456789", ShortcutLauncher.whatsAppNumber("+62 812-3456-789"))
    }

    // --- Two contexts (F-75) ---

    @Test
    fun idleDefaultsOpenControlsAndShortcuts() {
        assertEquals(CapsuleCardType.SLIDERS, gesture(loaded, GestureType.TAP).state.activeCard)
        assertEquals(CapsuleCardType.SHORTCUTS, gesture(loaded, GestureType.LONG_PRESS).state.activeCard)
        assertTrue(gesture(loaded, GestureType.SWIPE_LEFT).effects.isEmpty())
    }

    @Test
    fun activeContextUsesItsOwnMapping() {
        val withMedia = reduce(loaded, SystemEvent.MediaUpdated(media))
        assertTrue(withMedia.hasActivity)
        assertEquals(CapsuleMode.EXPANDED, gesture(withMedia, GestureType.TAP).state.mode)
        assertEquals(CapsuleCardType.MEDIA, gesture(withMedia, GestureType.TAP).state.activeCard)
        assertEquals(CapsuleCardType.SLIDERS, gesture(withMedia, GestureType.LONG_PRESS).state.activeCard)
    }

    @Test
    fun anOpenedCardIsNotActivity() {
        val shortcutsOpen = gesture(loaded, GestureType.LONG_PRESS).state
        assertFalse(shortcutsOpen.hasActivity)
        // The same gesture closes the card it opened.
        val closed = gesture(shortcutsOpen, GestureType.LONG_PRESS).state
        assertFalse(closed.isExpanded)
        assertNull(closed.openedCard)
    }

    // --- Actions ---

    private fun idle(type: GestureType, binding: GestureBinding) =
        loaded.copy(settings = Settings(gesturesIdle = mapOf(type to binding)))

    @Test
    fun screenshotHidesTheCapsuleUntilDone() {
        val reduction = gesture(idle(GestureType.DOUBLE_TAP, GestureBinding(GestureAction.SCREENSHOT)), GestureType.DOUBLE_TAP)
        assertEquals(listOf(Effect.Global(Effect.GlobalAction.SCREENSHOT)), reduction.effects)
        assertEquals(CapsuleMode.HIDDEN, reduction.state.mode)
        assertEquals(CapsuleMode.IDLE, reduce(reduction.state, SystemEvent.ScreenshotDone).mode)
    }

    @Test
    fun otherActionsEmitTheirEffects() {
        assertEquals(
            listOf(Effect.OpenApp("com.maps")),
            gesture(idle(GestureType.SWIPE_LEFT, GestureBinding(GestureAction.OPEN_APP, "com.maps")), GestureType.SWIPE_LEFT).effects
        )
        assertTrue(gesture(idle(GestureType.SWIPE_LEFT, GestureBinding(GestureAction.OPEN_APP)), GestureType.SWIPE_LEFT).effects.isEmpty())
        assertEquals(
            listOf(Effect.ScrollToTop),
            gesture(idle(GestureType.SWIPE_RIGHT, GestureBinding(GestureAction.SCROLL_TO_TOP)), GestureType.SWIPE_RIGHT).effects
        )
        assertEquals(
            listOf(Effect.Global(Effect.GlobalAction.LOCK_SCREEN)),
            gesture(idle(GestureType.DOUBLE_TAP, GestureBinding(GestureAction.LOCK_SCREEN)), GestureType.DOUBLE_TAP).effects
        )
    }

    @Test
    fun automationBroadcastIsAppendedForEveryGesture() {
        val state = loaded.copy(settings = Settings(automationBroadcast = true))
        val reduction = gesture(state, GestureType.SWIPE_LEFT)
        assertEquals(listOf(Effect.AutomationBroadcast("swipe_left", "none", null)), reduction.effects)
        assertTrue(gesture(loaded, GestureType.SWIPE_LEFT).effects.isEmpty())
    }

    @Test
    fun launchingAShortcutClosesTheCard() {
        val open = gesture(loaded, GestureType.LONG_PRESS).state
        val reduction = CapsuleReducer.reduce(open, UserIntent.LaunchShortcut("x"))
        assertEquals(listOf(Effect.LaunchShortcut("x")), reduction.effects)
        assertFalse(reduction.state.isExpanded)
    }
}
