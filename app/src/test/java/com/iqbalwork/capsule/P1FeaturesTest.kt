package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.ui.components.DISMISS_DRAG_DP
import com.iqbalwork.capsule.ui.components.dragFade
import com.iqbalwork.capsule.ui.components.shouldDismiss
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F-13 (media player allowlist) and F-23 (swipe to dismiss). */
class P1FeaturesTest {

    // --- F-13 ---

    @Test
    fun everyPlayerIsAllowedUnlessTheListIsSwitchedOn() {
        assertTrue(Settings().allowsMedia("com.any.player"))
        // A list that is not in force changes nothing.
        assertTrue(Settings(mediaAllowedApps = setOf("com.a")).allowsMedia("com.b"))
    }

    @Test
    fun withTheListOnlyListedPlayersAreAllowed() {
        val settings = Settings(mediaOnlyAllowedApps = true, mediaAllowedApps = setOf("com.spotify"))
        assertTrue(settings.allowsMedia("com.spotify"))
        assertFalse(settings.allowsMedia("com.other"))
    }

    @Test
    fun withTheListOnAndEmptyNoPlayerIsAllowed() {
        assertFalse(Settings(mediaOnlyAllowedApps = true).allowsMedia("com.spotify"))
    }

    @Test
    fun playerAllowlistAndSwipeSettingSurviveStorage() {
        val settings = Settings(
            mediaOnlyAllowedApps = true,
            mediaAllowedApps = setOf("com.a", "com.b"),
            notificationSwipeDismiss = true
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
    }

    @Test
    fun swipeToDismissIsOffByDefault() {
        assertFalse(Settings().notificationSwipeDismiss)
    }

    // --- F-23 ---

    private val threshold = DISMISS_DRAG_DP * 3f // any positive px value

    @Test
    fun dismissingNeedsADragPastTheThresholdInEitherDirection() {
        assertFalse(shouldDismiss(threshold - 1f, threshold))
        assertTrue(shouldDismiss(threshold, threshold))
        assertTrue(shouldDismiss(-threshold, threshold))
        assertFalse(shouldDismiss(0f, threshold))
    }

    @Test
    fun theCardFadesWithTheDragButNeverFullyBeforeItIsDismissed() {
        assertEquals(0f, dragFade(0f, threshold), 0f)
        assertEquals(dragFade(100f, threshold), dragFade(-100f, threshold), 0f)
        assertTrue(dragFade(threshold, threshold) > dragFade(threshold / 2, threshold))
        assertTrue(dragFade(10_000f, threshold) < 1f)
    }
}
