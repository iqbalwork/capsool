package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MAX_SCREEN_PROFILES
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.ScreenProfileKey
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.VisibilityContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** F-08: one calibration per panel x orientation, starting from the old per-orientation template. */
class ScreenProfileTest {

    private val fhd = ScreenProfileKey(1080, 2400, Orientation.PORTRAIT)
    private val qhd = ScreenProfileKey(1440, 3200, Orientation.PORTRAIT)
    private val mine = Geometry(offsetXDp = 12, offsetYDp = -3, widthDp = 150, heightDp = 40, cornerDp = 20)

    @Test
    fun rotatingKeepsThePanelAndOnlyChangesTheOrientation() {
        val portrait = ScreenProfileKey.of(1080, 2400, Orientation.PORTRAIT)
        val landscape = ScreenProfileKey.of(2400, 1080, Orientation.LANDSCAPE)
        assertEquals(portrait?.shortPx, landscape?.shortPx)
        assertEquals(portrait?.longPx, landscape?.longPx)
        assertEquals(portrait?.withOrientation(Orientation.LANDSCAPE), landscape)
    }

    @Test
    fun unknownScreenSizeHasNoProfile() {
        assertNull(ScreenProfileKey.of(0, 2400, Orientation.PORTRAIT))
        assertNull(VisibilityContext().profile)
    }

    @Test
    fun aScreenWithoutAProfileUsesTheOldTemplate() {
        val settings = Settings(portrait = mine)
        assertEquals(mine, settings.geometry(fhd, Orientation.PORTRAIT))
        assertEquals(mine, settings.geometry(null, Orientation.PORTRAIT))
    }

    @Test
    fun savingForOneScreenLeavesOtherScreensAlone() {
        val settings = Settings(portrait = Geometry()).withGeometry(fhd, Orientation.PORTRAIT, mine)
        assertEquals(mine, settings.geometry(fhd, Orientation.PORTRAIT))
        assertEquals(Geometry(), settings.geometry(qhd, Orientation.PORTRAIT))
    }

    @Test
    fun withoutAProfileTheTemplateIsTheThingEdited() {
        val settings = Settings().withGeometry(null, Orientation.LANDSCAPE, mine)
        assertEquals(mine, settings.landscape)
        assertEquals(emptyMap<ScreenProfileKey, Geometry>(), settings.profiles)
    }

    @Test
    fun removingAProfileFallsBackToTheTemplate() {
        val settings = Settings(portrait = Geometry(widthDp = 99)).withGeometry(fhd, Orientation.PORTRAIT, mine)
        assertEquals(99, settings.withoutProfile(fhd).geometry(fhd, Orientation.PORTRAIT).widthDp)
    }

    @Test
    fun theOldestProfileIsForgottenPastTheLimit() {
        var settings = Settings()
        val keys = (1..MAX_SCREEN_PROFILES + 3).map { ScreenProfileKey(1000 + it, 2400, Orientation.PORTRAIT) }
        keys.forEach { settings = settings.withGeometry(it, Orientation.PORTRAIT, mine) }
        assertEquals(MAX_SCREEN_PROFILES, settings.profiles.size)
        assertNull(settings.profiles[keys.first()])
        assertNotNull(settings.profiles[keys.last()])
    }

    @Test
    fun theStateUsesTheProfileOfTheCurrentScreenAndRotation() {
        val settings = Settings().withGeometry(fhd, Orientation.PORTRAIT, mine)
        val onFhd = CapsuleState(settings = settings, visibility = VisibilityContext(screenWidthPx = 1080, screenHeightPx = 2400))
        val onQhd = onFhd.copy(visibility = VisibilityContext(screenWidthPx = 1440, screenHeightPx = 3200))
        val rotated = onFhd.copy(
            visibility = VisibilityContext(screenWidthPx = 2400, screenHeightPx = 1080, orientation = Orientation.LANDSCAPE)
        )
        assertEquals(mine, onFhd.geometry)
        assertEquals(Geometry(), onQhd.geometry)
        assertEquals(Geometry(), rotated.geometry)
    }

    @Test
    fun profilesSurviveStorageAndBadLinesAreSkipped() {
        val settings = Settings().withGeometry(fhd, Orientation.PORTRAIT, mine).withGeometry(qhd, Orientation.PORTRAIT, Geometry())
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings.profiles, SettingsMapper.read(prefs).profiles)

        val damaged = mutablePreferencesOf(
            stringPreferencesKey("geometry.profiles") to "garbage\n1080x2400:PORTRAIT;1;2;3\n1080x2400:SIDEWAYS;1;2;3;4;5\n1080x2400:PORTRAIT;1;2;3;4;5"
        )
        assertEquals(mapOf(fhd to Geometry(1, 2, 3, 4, 5)), SettingsMapper.read(damaged).profiles)
    }
}
