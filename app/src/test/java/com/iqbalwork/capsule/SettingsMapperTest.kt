package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.iqbalwork.capsule.core.settings.ColorSource
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MediaCollapseDelay
import com.iqbalwork.capsule.core.settings.MiniEventDuration
import com.iqbalwork.capsule.core.settings.LockScreenBehavior
import com.iqbalwork.capsule.model.EventCategory
import com.iqbalwork.capsule.core.settings.NotificationDelay
import com.iqbalwork.capsule.core.settings.NotificationDisplay
import com.iqbalwork.capsule.core.settings.Settings
import org.junit.Assert.assertNull
import com.iqbalwork.capsule.core.settings.MAX_BORDER_DP
import com.iqbalwork.capsule.core.settings.MAX_ANIMATION_SPEED
import com.iqbalwork.capsule.core.settings.ExpandStyle
import com.iqbalwork.capsule.core.settings.CapsuleShapeStyle
import com.iqbalwork.capsule.core.settings.BounceLevel
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.settings.SliderStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMapperTest {

    @Test
    fun emptyPreferencesGiveDefaults() {
        assertEquals(Settings(), SettingsMapper.read(mutablePreferencesOf()))
    }

    @Test
    fun roundTripKeepsEveryField() {
        val settings = Settings(
            portrait = Geometry(offsetXDp = -12, offsetYDp = 3, widthDp = 140, heightDp = 40, cornerDp = 20),
            landscape = Geometry(offsetXDp = 5),
            idleOpacity = 0.4f,
            alwaysShowPill = false,
            showCalibrationTarget = true,
            colorSource = ColorSource.BRAND,
            hapticsEnabled = false,
            hideInFullscreen = false,
            hideWithKeyboard = false,
            hideInLandscape = false,
            gesturesActive = GestureType.entries.associateWith { GestureBinding(GestureAction.OPEN_QUICK_SETTINGS) },
            gesturesIdle = GestureType.entries.associateWith { GestureBinding(GestureAction.OPEN_APP, "com.example.app") },
            automationBroadcast = true,
            shortcuts = listOf(
                Shortcut("1", ShortcutKind.APP, "Maps", "com.google.android.apps.maps"),
                Shortcut("2", ShortcutKind.CONTACT, "Mom", "+62 812-3456", ContactAction.WHATSAPP),
                Shortcut("3", ShortcutKind.SYSTEM, "Wi-Fi", "WIFI_PANEL")
            ),
            themedShortcutIcons = false,
            mediaEnabled = false,
            mediaCollapseDelay = MediaCollapseDelay.NEVER,
            hideMediaWhenAppInFront = false,
            notificationsEnabled = false,
            notificationDisplay = NotificationDisplay.EXPANDED,
            notificationDelay = NotificationDelay.UNTIL_OPENED,
            notificationBlockedApps = setOf("com.a", "com.b"),
            hiddenPackages = setOf("com.hide.me"),
            lockScreenBehavior = LockScreenBehavior.HIDE,
            removeNotificationAfterViewing = true,
            notificationGlow = false,
            chargingCardEnabled = false,
            enabledEvents = setOf(EventCategory.WIFI, EventCategory.UNLOCK),
            miniEventDuration = MiniEventDuration.SECONDS_6,
            lowBatteryThreshold = 22,
            temperatureFahrenheit = true,
            sliderStyle = SliderStyle.ROUNDED,
            brightnessMax = 4095,
            progressEnabled = false,
            timerEnabled = false,
            disclosureAccepted = true
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
    }

    @Test
    fun legacySingleGestureMapBecomesTheActiveSet() {
        val prefs = mutablePreferencesOf(
            androidx.datastore.preferences.core.stringPreferencesKey("gestures.long_press") to "OPEN_QUICK_SETTINGS"
        )
        val settings = SettingsMapper.read(prefs)
        assertEquals(GestureAction.OPEN_QUICK_SETTINGS, settings.gesture(GestureType.LONG_PRESS, active = true).action)
        assertEquals(GestureAction.OPEN_SHORTCUTS, settings.gesture(GestureType.LONG_PRESS, active = false).action)
    }

    // --- F-85 expand phase: the legacy lock-screen boolean migrates into the behaviour enum ---

    private val legacyHideTextKey =
        androidx.datastore.preferences.core.booleanPreferencesKey("notification.hideContentWhenLocked")
    private val newLockScreenKey =
        androidx.datastore.preferences.core.stringPreferencesKey("visibility.lockScreenBehavior")

    @Test
    fun aDeviceThatOnlyHasTheLegacySwitchKeepsHidingText() {
        val prefs = mutablePreferencesOf(legacyHideTextKey to true)
        assertEquals(LockScreenBehavior.SHOW_WITHOUT_CONTENT, SettingsMapper.read(prefs).lockScreenBehavior)
    }

    @Test
    fun aDeviceThatHadTheLegacySwitchOffStartsShowingText() {
        val prefs = mutablePreferencesOf(legacyHideTextKey to false)
        assertEquals(LockScreenBehavior.SHOW, SettingsMapper.read(prefs).lockScreenBehavior)
    }

    @Test
    fun theNewKeyWinsOverTheLegacyOne() {
        val prefs = mutablePreferencesOf(
            legacyHideTextKey to true,
            newLockScreenKey to LockScreenBehavior.HIDE.name
        )
        assertEquals(LockScreenBehavior.HIDE, SettingsMapper.read(prefs).lockScreenBehavior)
    }

    @Test
    fun writingKeepsTheLegacyKeyInStepForOlderBuilds() {
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, Settings(lockScreenBehavior = LockScreenBehavior.SHOW))
        // An older build can only read the boolean, and must not keep hiding text the user revealed.
        assertEquals(false, prefs[legacyHideTextKey])

        val hidden = mutablePreferencesOf()
        SettingsMapper.write(hidden, Settings(lockScreenBehavior = LockScreenBehavior.HIDE))
        assertEquals(true, hidden[legacyHideTextKey])
    }

    @Test
    fun appearanceAndAnimationSettingsRoundTrip() {
        val settings = Settings(
            shapeStyle = CapsuleShapeStyle.SQUIRCLE,
            customBackground = 0xFF1B2A41L,
            borderDp = 2,
            borderColor = 0xFFFFB690L,
            shadowDp = 4,
            animationSpeed = 1.5f,
            bounce = BounceLevel.HIGH,
            expandStyle = ExpandStyle.RIGID,
            reduceAnimations = true
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
    }

    @Test
    fun clearingTheCustomColorsRemovesThemFromStorage() {
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, Settings(customBackground = 0xFF1B2A41L, borderColor = 0xFFFFFFFFL))
        SettingsMapper.write(prefs, Settings(customBackground = null, borderColor = null))
        val read = SettingsMapper.read(prefs)
        assertNull(read.customBackground)
        assertNull(read.borderColor)
    }

    @Test
    fun outOfRangeStoredValuesAreClamped() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("appearance.border") to 40,
            intPreferencesKey("appearance.shadow") to -3,
            floatPreferencesKey("animation.speed") to 9f
        )
        val read = SettingsMapper.read(prefs)
        assertEquals(MAX_BORDER_DP, read.borderDp)
        assertEquals(0, read.shadowDp)
        assertEquals(MAX_ANIMATION_SPEED, read.animationSpeed, 0f)
    }
}
