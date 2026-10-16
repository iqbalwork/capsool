package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.LockScreenBehavior
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.VisibilityContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F-84 (stay hidden in chosen apps) and F-85 (lock screen behaviour).
 *
 * Both are rules about whether the capsule may be on screen at all, so both live on
 * [CapsuleState.isAllowedOnScreen] — the single place that decision is made. Testing them here
 * rather than through the overlay keeps the rule honest without needing a device.
 */
class VisibilityRulesTest {

    private fun state(
        foregroundPackage: String? = null,
        locked: Boolean = false,
        hiddenPackages: Set<String> = emptySet(),
        lockScreenBehavior: LockScreenBehavior = LockScreenBehavior.SHOW_WITHOUT_CONTENT
    ) = CapsuleState(
        settingsLoaded = true,
        settings = Settings(
            hiddenPackages = hiddenPackages,
            lockScreenBehavior = lockScreenBehavior
        ),
        visibility = VisibilityContext(foregroundPackage = foregroundPackage, locked = locked)
    )

    // --- F-84: hide list ---

    @Test
    fun anAppOnTheHideListKeepsTheCapsuleOffScreen() {
        val capsule = state(foregroundPackage = "com.game", hiddenPackages = setOf("com.game"))
        assertFalse(capsule.isAllowedOnScreen)
        assertEquals(CapsuleMode.HIDDEN, capsule.mode)
    }

    @Test
    fun anAppThatIsNotOnTheHideListIsUnaffected() {
        val capsule = state(foregroundPackage = "com.chat", hiddenPackages = setOf("com.game"))
        assertTrue(capsule.isAllowedOnScreen)
    }

    @Test
    fun anEmptyHideListHidesNothing() {
        assertTrue(state(foregroundPackage = "com.chat").isAllowedOnScreen)
    }

    @Test
    fun anUnresolvedForegroundAppNeverCountsAsHidden() {
        // Before the service has reported an app there is nothing to match against; a hide list
        // must not swallow the capsule in that window.
        val capsule = state(foregroundPackage = null, hiddenPackages = setOf("com.game"))
        assertTrue(capsule.isAllowedOnScreen)
    }

    // --- F-85: lock screen ---

    @Test
    fun hideRemovesTheCapsuleWhileLocked() {
        val capsule = state(locked = true, lockScreenBehavior = LockScreenBehavior.HIDE)
        assertFalse(capsule.isAllowedOnScreen)
        assertEquals(CapsuleMode.HIDDEN, capsule.mode)
    }

    @Test
    fun hideStillShowsTheCapsuleOnceUnlocked() {
        assertTrue(state(locked = false, lockScreenBehavior = LockScreenBehavior.HIDE).isAllowedOnScreen)
    }

    @Test
    fun showWithoutContentStillShowsTheCapsuleWhileLocked() {
        assertTrue(
            state(locked = true, lockScreenBehavior = LockScreenBehavior.SHOW_WITHOUT_CONTENT)
                .isAllowedOnScreen
        )
    }

    @Test
    fun showKeepsTheCapsuleWhileLocked() {
        assertTrue(state(locked = true, lockScreenBehavior = LockScreenBehavior.SHOW).isAllowedOnScreen)
    }

    @Test
    fun theHideListWinsOverALockScreenSettingThatWouldShow() {
        // Both rules are satisfied-and-denied independently; hiding in an app must not be undone
        // just because the lock-screen setting says "show".
        val capsule = state(
            foregroundPackage = "com.game",
            hiddenPackages = setOf("com.game"),
            locked = false,
            lockScreenBehavior = LockScreenBehavior.SHOW
        )
        assertFalse(capsule.isAllowedOnScreen)
    }
}
