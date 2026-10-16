package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbalwork.capsule.core.settings.DEFAULT_MINI_EXTRA_WIDTH_DP
import com.iqbalwork.capsule.core.settings.DEFAULT_MINI_ROW_DP
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MiniExtraWidthRange
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.ScreenProfileKey
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.platform.accessibility.CutoutBounds
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.ui.overlay.tapSwitchAction
import org.junit.Assert.assertEquals
import org.junit.Test

/** The pop-up event pill ("Mini") calibrated on its own, and tap-to-switch on the dots strip. */
class MiniGeometryTest {
    private val density = 3f
    private val cutout = CutoutBounds(left = 510, top = 60, right = 570, bottom = 120)
    private val fhd = ScreenProfileKey(1080, 2400, Orientation.PORTRAIT)

    @Test
    fun theMiniDefaultsAreTheValuesThePillAlwaysHad() {
        val g = Geometry()
        assertEquals(DEFAULT_MINI_EXTRA_WIDTH_DP, g.miniExtraWidthDp)
        assertEquals(DEFAULT_MINI_ROW_DP, g.miniRowDp)
        assertEquals(0, g.miniOffsetXDp)
        assertEquals(120 + 110, OverlayPlacement.pillWidthDp(g, miniEvent = true))
    }

    @Test
    fun theEventPillUsesItsOwnWidthAndRow() {
        val g = Geometry(miniExtraWidthDp = 160, miniRowDp = 40)
        assertEquals(120 + 160, OverlayPlacement.pillWidthDp(g, miniEvent = true))
        val p = OverlayPlacement.compute(1080, density, cutout, g, expanded = false, miniEvent = true)
        assertEquals(40, p.eventRowDp)
        // Not showing an event, neither applies.
        assertEquals(120, OverlayPlacement.pillWidthDp(g, miniEvent = false))
        assertEquals(0, OverlayPlacement.compute(1080, density, cutout, g, expanded = false).eventRowDp)
    }

    @Test
    fun theMiniShiftMovesTheEventPillOnly() {
        val g = Geometry(miniOffsetXDp = 20)
        val idle = OverlayPlacement.compute(1080, density, cutout, g, expanded = false)
        val plainIdle = OverlayPlacement.compute(1080, density, cutout, Geometry(), expanded = false)
        assertEquals(plainIdle.x, idle.x)
        val event = OverlayPlacement.compute(1080, density, cutout, g, expanded = false, miniEvent = true)
        val plainEvent = OverlayPlacement.compute(1080, density, cutout, Geometry(), expanded = false, miniEvent = true)
        assertEquals(plainEvent.x + (20 * density).toInt(), event.x)
    }

    @Test
    fun miniSettingsSurviveStorageInTheTemplateAndInProfiles() {
        val mini = Geometry(widthDp = 100, miniExtraWidthDp = 150, miniRowDp = 36, miniOffsetXDp = -12)
        val settings = Settings(portrait = mini).withGeometry(fhd, Orientation.PORTRAIT, mini.copy(miniRowDp = 44))
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        assertEquals(settings, SettingsMapper.read(prefs))
    }

    @Test
    fun profilesWrittenBeforeMiniExistedStillLoadWithTheDefaults() {
        val old = mutablePreferencesOf(stringPreferencesKey("geometry.profiles") to "1080x2400:PORTRAIT;5;6;130;40;20")
        assertEquals(
            Geometry(5, 6, 130, 40, 20),
            SettingsMapper.read(old).profiles.getValue(fhd)
        )
    }

    @Test
    fun outOfRangeStoredMiniValuesAreClamped() {
        val wild = mutablePreferencesOf(stringPreferencesKey("geometry.profiles") to "1080x2400:PORTRAIT;0;0;120;36;18;9999;1;500")
        val g = SettingsMapper.read(wild).profiles.getValue(fhd)
        assertEquals(MiniExtraWidthRange.last, g.miniExtraWidthDp)
        assertEquals(20, g.miniRowDp)
        assertEquals(60, g.miniOffsetXDp)
    }

    // --- tap strip ---

    @Test
    fun tappingTheLeftHalfGoesBackAndTheRightHalfGoesOn() {
        assertEquals(GestureAction.PREVIOUS_CARD, tapSwitchAction(10f, 300f))
        assertEquals(GestureAction.PREVIOUS_CARD, tapSwitchAction(149f, 300f))
        assertEquals(GestureAction.NEXT_CARD, tapSwitchAction(150f, 300f))
        assertEquals(GestureAction.NEXT_CARD, tapSwitchAction(290f, 300f))
    }

    @Test
    fun tapSwitchIsOffByDefaultAndSurvivesStorage() {
        assertEquals(false, Settings().cardTapSwitch)
        val on = Settings(cardTapSwitch = true)
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, on)
        assertEquals(on, SettingsMapper.read(prefs))
    }
}
