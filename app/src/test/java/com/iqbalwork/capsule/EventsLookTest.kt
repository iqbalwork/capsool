package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbalwork.capsule.core.settings.EventLeftShape
import com.iqbalwork.capsule.core.settings.EventRightShape
import com.iqbalwork.capsule.core.settings.EventStyle
import com.iqbalwork.capsule.core.settings.ProgressIconStyle
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.EventCategory
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.ProgressItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F-43 (progress apps and style), F-68 (per-event look) and feedback for a failed gesture action. */
class EventsLookTest {

    private fun progress(pkg: String) = ProgressItem(
        key = pkg, packageName = pkg, appName = pkg, title = "file", fraction = 0.5f, color = 0xFF00FF00
    )

    private fun reduce(state: CapsuleState, event: SystemEvent) = CapsuleReducer.reduce(state, event).state

    // --- F-43 ---

    @Test
    fun everyAppShowsProgressUnlessTheListIsOn() {
        assertTrue(Settings().allowsProgress("com.any"))
        assertTrue(Settings(progressAllowedApps = setOf("com.a")).allowsProgress("com.b"))
    }

    @Test
    fun withTheListOnlyListedAppsShowProgress() {
        val state = CapsuleState(
            settingsLoaded = true,
            settings = Settings(progressOnlyAllowedApps = true, progressAllowedApps = setOf("com.ok"))
        )
        val after = reduce(reduce(state, SystemEvent.ProgressUpdated(progress("com.ok"))), SystemEvent.ProgressUpdated(progress("com.no")))
        assertEquals(listOf("com.ok"), after.progress.map { it.packageName })
    }

    @Test
    fun narrowingTheListDropsWhatIsAlreadyShown() {
        val open = reduce(CapsuleState(settingsLoaded = true), SystemEvent.ProgressUpdated(progress("com.a")))
        val after = reduce(open, SystemEvent.SettingsChanged(Settings(progressOnlyAllowedApps = true)))
        assertTrue(after.progress.isEmpty())
    }

    @Test
    fun progressStyleSettingsSurviveStorage() {
        val settings = Settings(
            progressOnlyAllowedApps = true,
            progressAllowedApps = setOf("com.a"),
            progressIconStyle = ProgressIconStyle.SHAPE,
            progressColor = 0xFF7FC8F8,
            progressTrackColor = 0xFF2A2A2A
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
        // Clearing a color removes it rather than leaving the old one behind.
        SettingsMapper.write(prefs, settings.copy(progressColor = null, progressTrackColor = null))
        assertNull(SettingsMapper.read(prefs).progressColor)
        assertNull(SettingsMapper.read(prefs).progressTrackColor)
    }

    // --- F-68 ---

    @Test
    fun anEventWithoutAStyleIsShownAsDesigned() {
        assertEquals(EventStyle.Default, Settings().eventStyle(EventCategory.WIFI))
        assertEquals(EventStyle.Default, Settings().eventStyle(null))
    }

    @Test
    fun eventStylesSurviveStorageAndDefaultsAreNotWritten() {
        val styled = EventStyle(color = 0xFFFFB690, icon = "star", left = EventLeftShape.SQUARE, right = EventRightShape.TEXT)
        val settings = Settings(eventStyles = mapOf(EventCategory.WIFI to styled, EventCategory.RINGER to EventStyle.Default))
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(mapOf(EventCategory.WIFI to styled), SettingsMapper.read(prefs).eventStyles)
    }

    @Test
    fun damagedEventStyleLinesAreSkipped() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("events.styles") to
                "NOPE;;;CIRCLE;AUTO\nWIFI;abc;;CIRCLE;AUTO\nWIFI;;;TRIANGLE;AUTO\nWIFI;;;SQUARE\nRINGER;;bolt;NONE;NONE"
        )
        assertEquals(
            mapOf(EventCategory.RINGER to EventStyle(icon = "bolt", left = EventLeftShape.NONE, right = EventRightShape.NONE)),
            SettingsMapper.read(prefs).eventStyles
        )
    }

    // --- failed gesture action ---

    @Test
    fun aFailedActionIsShownEvenWhenEveryPopUpIsSwitchedOff() {
        val quiet = CapsuleState(settingsLoaded = true, settings = Settings(enabledEvents = emptySet()))
        val failed = MiniEvent(1L, MiniEventType.ACTION_FAILED, "Action failed", "Open Tasker")
        assertNotNull(reduce(quiet, SystemEvent.MiniEventStarted(failed)).miniEvent)
        // A normal event is still filtered out by the same settings.
        val wifi = MiniEvent(2L, MiniEventType.WIFI_CONNECTED, "Wi-Fi")
        assertNull(reduce(quiet, SystemEvent.MiniEventStarted(wifi)).miniEvent)
    }

    @Test
    fun eventDisplayDefaultsToBelowCameraAndSurvivesStorage() {
        assertEquals(com.iqbalwork.capsule.core.settings.EventDisplay.BELOW_CAMERA, Settings().eventDisplay)
        val split = Settings(eventDisplay = com.iqbalwork.capsule.core.settings.EventDisplay.SPLIT)
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, split)
        assertEquals(split, SettingsMapper.read(prefs))
    }
}
