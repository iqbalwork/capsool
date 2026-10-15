package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TorchState
import com.iqbalwork.capsule.platform.controls.BrightnessCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsTest {

    private val loaded = CapsuleState(settingsLoaded = true)
    private val sliders = SliderState(canSilence = true, canWriteBrightness = true, torch = TorchState.OFF)
    private val withControls = loaded.copy(sliders = sliders)

    private fun reduce(state: CapsuleState, vararg actions: Action) =
        actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    // --- Brightness curve ---

    @Test
    fun curveEndpointsAndMidpoint() {
        assertEquals(0f, BrightnessCurve.positionFor(1, 1, 255), 0.001f)
        assertEquals(1f, BrightnessCurve.positionFor(255, 1, 255), 0.001f)
        assertEquals(1, BrightnessCurve.valueFor(0f, 1, 255))
        assertEquals(255, BrightnessCurve.valueFor(1f, 1, 255))
        // Half the slider is far less than half the raw value: that is the point of the curve.
        assertTrue(BrightnessCurve.valueFor(0.5f, 1, 255) < 60)
    }

    @Test
    fun curveIsMonotonicAndRoundTrips() {
        var previous = -1
        for (step in 0..100) {
            val value = BrightnessCurve.valueFor(step / 100f, 1, 4095)
            assertTrue(value >= previous)
            previous = value
        }
        for (raw in listOf(1, 10, 100, 1000, 2047, 4095)) {
            val back = BrightnessCurve.valueFor(BrightnessCurve.positionFor(raw, 1, 4095), 1, 4095)
            assertTrue("raw=$raw back=$back", kotlin.math.abs(back - raw) <= maxOf(2, raw / 100))
        }
    }

    // --- Controls card ---

    @Test
    fun openingControlsRereadsPermissions() {
        assertEquals(listOf(Effect.RefreshControls), CapsuleReducer.reduce(withControls, UserIntent.Gesture(GestureType.TAP)).effects)
    }

    @Test
    fun controlsCloseWithTheCard() {
        val open = reduce(withControls, UserIntent.Gesture(GestureType.LONG_PRESS))
        assertEquals(CapsuleCardType.SLIDERS, open.activeCard)
        val closed = reduce(open, UserIntent.Collapse)
        assertFalse(closed.controlsOpen)
        assertTrue(closed.activeCards.isEmpty())
    }

    @Test
    fun longPressOpensControlsEvenWithOtherActivity() {
        val media = MediaState(packageName = "p", title = "t", isPlaying = true)
        val state = reduce(withControls, SystemEvent.MediaUpdated(media), UserIntent.Gesture(GestureType.LONG_PRESS))
        assertEquals(CapsuleCardType.SLIDERS, state.activeCard)
        assertEquals(listOf(CapsuleCardType.MEDIA, CapsuleCardType.SLIDERS), state.activeCards)
    }

    @Test
    fun torchOnShowsControlsAsActivity() {
        val torchOn = reduce(withControls, SystemEvent.SlidersUpdated(sliders.copy(torch = TorchState.ON)))
        assertEquals(CapsuleMode.COMPACT, torchOn.mode)
        assertEquals(CapsuleCardType.SLIDERS, torchOn.activeCard)
    }

    // --- Ringer ---

    @Test
    fun ringerCyclesThroughSilentWithAccess() {
        val vibrate = CapsuleReducer.reduce(withControls, UserIntent.CycleRinger)
        assertEquals(RingerMode.VIBRATE, vibrate.state.sliders.ringerMode)
        val silent = CapsuleReducer.reduce(vibrate.state, UserIntent.CycleRinger)
        assertEquals(listOf(Effect.Ringer(RingerMode.SILENT)), silent.effects)
        assertEquals(RingerMode.NORMAL, CapsuleReducer.reduce(silent.state, UserIntent.CycleRinger).state.sliders.ringerMode)
    }

    @Test
    fun ringerSkipsSilentAndAsksWithoutAccess() {
        val state = withControls.copy(sliders = sliders.copy(canSilence = false, ringerMode = RingerMode.VIBRATE), isExpanded = true)
        val reduction = CapsuleReducer.reduce(state, UserIntent.CycleRinger)
        assertEquals(RingerMode.NORMAL, reduction.state.sliders.ringerMode)
        assertEquals(
            listOf(Effect.Ringer(RingerMode.NORMAL), Effect.OpenPermission(Effect.PermissionScreen.DND_ACCESS)),
            reduction.effects
        )
        assertFalse(reduction.state.isExpanded)
    }

    // --- Brightness ---

    @Test
    fun brightnessWithoutPermissionAsksForIt() {
        val state = withControls.copy(sliders = sliders.copy(canWriteBrightness = false))
        val reduction = CapsuleReducer.reduce(state, UserIntent.SetBrightness(0.7f))
        assertEquals(listOf(Effect.OpenPermission(Effect.PermissionScreen.WRITE_SETTINGS)), reduction.effects)
        assertEquals(0f, reduction.state.sliders.brightnessLevel, 0f)
        assertEquals(
            listOf(Effect.OpenPermission(Effect.PermissionScreen.WRITE_SETTINGS)),
            CapsuleReducer.reduce(state, UserIntent.ToggleAdaptiveBrightness).effects
        )
    }

    @Test
    fun brightnessAndAdaptiveWithPermission() {
        assertEquals(listOf(Effect.Brightness(0.7f)), CapsuleReducer.reduce(withControls, UserIntent.SetBrightness(0.7f)).effects)
        val adaptive = CapsuleReducer.reduce(withControls, UserIntent.ToggleAdaptiveBrightness)
        assertTrue(adaptive.state.sliders.isAdaptiveBrightness)
        assertEquals(listOf(Effect.AdaptiveBrightness(true)), adaptive.effects)
    }

    // --- Torch ---

    @Test
    fun torchToggleAndStrength() {
        val on = CapsuleReducer.reduce(withControls, UserIntent.ToggleTorch)
        assertEquals(TorchState.ON, on.state.sliders.torch)
        assertEquals(listOf(Effect.Torch(true, null)), on.effects)

        val strong = withControls.copy(sliders = sliders.copy(torchStrengthSupported = true, torchStrengthLevel = 0.4f))
        assertEquals(listOf(Effect.Torch(true, 0.4f)), CapsuleReducer.reduce(strong, UserIntent.ToggleTorch).effects)

        val dimmedToOff = CapsuleReducer.reduce(strong, UserIntent.SetTorchLevel(0.01f))
        assertEquals(TorchState.OFF, dimmedToOff.state.sliders.torch)
        assertEquals(listOf(Effect.Torch(false, null)), dimmedToOff.effects)
    }

    @Test
    fun unavailableTorchIgnoresInput() {
        for (torch in listOf(TorchState.NONE, TorchState.BUSY)) {
            val state = withControls.copy(sliders = sliders.copy(torch = torch))
            assertTrue(CapsuleReducer.reduce(state, UserIntent.ToggleTorch).effects.isEmpty())
            assertTrue(CapsuleReducer.reduce(state, UserIntent.SetTorchLevel(0.8f)).effects.isEmpty())
        }
    }
}
