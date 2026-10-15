package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.CapsuleMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.model.EventCategory
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.platform.accessibility.OverlayPlacement
import com.iqbalwork.capsule.platform.system.BatteryEventDetector
import com.iqbalwork.capsule.platform.system.WifiTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemEventsTest {

    private fun battery(level: Int, charging: Boolean = false, full: Boolean = false) = ChargingState(
        isCharging = charging,
        chargePercentage = level,
        chargeType = if (charging) ChargingType.AC else ChargingType.NONE,
        isFullCharged = full
    )

    // --- Battery ---

    @Test
    fun firstReadingIsOnlyABaseline() {
        assertTrue(BatteryEventDetector.detect(null, battery(3, charging = true), 15).isEmpty())
    }

    @Test
    fun plugAndUnplug() {
        assertEquals(listOf(MiniEventType.CHARGER_CONNECTED), BatteryEventDetector.detect(battery(50), battery(50, true), 15))
        assertEquals(listOf(MiniEventType.CHARGER_DISCONNECTED), BatteryEventDetector.detect(battery(50, true), battery(50), 15))
    }

    @Test
    fun fullOnlyOnceWhilePlugged() {
        assertEquals(
            listOf(MiniEventType.FULLY_CHARGED),
            BatteryEventDetector.detect(battery(99, true), battery(100, true, full = true), 15)
        )
        assertTrue(BatteryEventDetector.detect(battery(100, true, true), battery(100, true, true), 15).isEmpty())
        // Plugging into an already full battery is "charging", not "fully charged" again.
        assertEquals(
            listOf(MiniEventType.CHARGER_CONNECTED),
            BatteryEventDetector.detect(battery(100), battery(100, true, full = true), 15)
        )
    }

    @Test
    fun lowBatteryWhenCrossingThresholdAndCriticalLevel() {
        assertEquals(listOf(MiniEventType.LOW_BATTERY), BatteryEventDetector.detect(battery(16), battery(15), 15))
        assertTrue(BatteryEventDetector.detect(battery(15), battery(14), 15).isEmpty())
        assertEquals(listOf(MiniEventType.LOW_BATTERY), BatteryEventDetector.detect(battery(6), battery(5), 15))
        assertTrue(BatteryEventDetector.detect(battery(16, true), battery(15, true), 15).isEmpty())
        // Threshold 5: one warning, not two.
        assertEquals(listOf(MiniEventType.LOW_BATTERY), BatteryEventDetector.detect(battery(6), battery(5), 5))
    }

    @Test
    fun temperatureJitterIsNotAVisibleChange() {
        val a = battery(50).copy(batteryTemperatureC = 30.0f)
        assertFalse(BatteryEventDetector.isVisibleChange(a, a.copy(batteryTemperatureC = 30.4f)))
        assertTrue(BatteryEventDetector.isVisibleChange(a, a.copy(batteryTemperatureC = 31.0f)))
        assertTrue(BatteryEventDetector.isVisibleChange(a, a.copy(chargePercentage = 49)))
    }

    // --- Wi-Fi ---

    @Test
    fun wifiAlreadyConnectedAtStartIsNotAnnounced() {
        val tracker = WifiTracker().apply { reset(nowMs = 0) }
        assertNull(tracker.onAvailable("net1", nowMs = 100))
        assertEquals(false, tracker.onLost("net1"))
        assertEquals(true, tracker.onAvailable("net1", nowMs = 5_000))
    }

    @Test
    fun wifiConnectingLaterIsAnnouncedOnce() {
        val tracker = WifiTracker().apply { reset(nowMs = 0) }
        assertEquals(true, tracker.onAvailable("a", nowMs = 10_000))
        assertNull(tracker.onAvailable("b", nowMs = 10_100))
        assertNull(tracker.onLost("a"))
        assertEquals(false, tracker.onLost("b"))
        assertNull(tracker.onLost("unknown"))
    }

    // --- Store ---

    private val loaded = CapsuleState(settingsLoaded = true)

    @Test
    fun disabledEventCategoriesAreIgnored() {
        val event = MiniEvent(1L, MiniEventType.DEVICE_UNLOCKED, "Welcome back")
        assertNull(CapsuleReducer.reduce(loaded, SystemEvent.MiniEventStarted(event)).state.miniEvent)
        val allowed = loaded.copy(settings = Settings(enabledEvents = setOf(EventCategory.UNLOCK)))
        assertEquals(CapsuleMode.MINI_EVENT, CapsuleReducer.reduce(allowed, SystemEvent.MiniEventStarted(event)).state.mode)
    }

    @Test
    fun chargingCardFollowsSetting() {
        val charging = CapsuleReducer.reduce(loaded, SystemEvent.ChargingUpdated(battery(40, true))).state
        assertEquals(listOf(CapsuleCardType.CHARGING), charging.activeCards)
        assertTrue(charging.copy(settings = Settings(chargingCardEnabled = false)).activeCards.isEmpty())
    }

    @Test
    fun miniEventWidensThePill() {
        val geometry = Geometry(widthDp = 120)
        assertEquals(120, OverlayPlacement.pillWidthDp(geometry, miniEvent = false))
        assertEquals(120 + OverlayPlacement.MINI_EVENT_EXTRA_WIDTH_DP, OverlayPlacement.pillWidthDp(geometry, miniEvent = true))
        val window = OverlayPlacement.compute(1080, 3f, null, geometry, expanded = true)
        val rect = OverlayPlacement.capsuleRect(window, 3f, geometry, expanded = false, miniEvent = true)
        assertEquals((120 + OverlayPlacement.MINI_EVENT_EXTRA_WIDTH_DP) * 3, rect.right - rect.left)
    }
}
