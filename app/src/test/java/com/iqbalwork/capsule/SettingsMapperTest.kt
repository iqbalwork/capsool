package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.iqbalwork.capsule.core.settings.ColorSource
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MediaCollapseDelay
import com.iqbalwork.capsule.core.settings.MiniEventDuration
import com.iqbalwork.capsule.model.EventCategory
import com.iqbalwork.capsule.core.settings.NotificationDelay
import com.iqbalwork.capsule.core.settings.NotificationDisplay
import com.iqbalwork.capsule.core.settings.Settings
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
            gestures = GestureType.entries.associateWith { GestureAction.OPEN_QUICK_SETTINGS },
            mediaEnabled = false,
            mediaCollapseDelay = MediaCollapseDelay.NEVER,
            hideMediaWhenAppInFront = false,
            notificationsEnabled = false,
            notificationDisplay = NotificationDisplay.EXPANDED,
            notificationDelay = NotificationDelay.UNTIL_OPENED,
            notificationBlockedApps = setOf("com.a", "com.b"),
            hideNotificationTextWhenLocked = false,
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
}
