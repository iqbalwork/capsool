package com.iqbalwork.capsule.platform.system

import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.model.MiniEventType

/** Below this the user is warned again even if their own threshold was already announced. */
const val CRITICAL_BATTERY_LEVEL = 5

/**
 * Turns consecutive battery readings into mini events (spec §2.5.3). Pure: no Android types,
 * so it is unit-tested directly. The first reading only sets the baseline.
 */
object BatteryEventDetector {

    fun detect(previous: ChargingState?, current: ChargingState, lowThreshold: Int): List<MiniEventType> {
        if (previous == null) return emptyList()
        val events = mutableListOf<MiniEventType>()
        when {
            !previous.isCharging && current.isCharging -> events += MiniEventType.CHARGER_CONNECTED
            previous.isCharging && !current.isCharging -> events += MiniEventType.CHARGER_DISCONNECTED
        }
        if (current.isCharging && current.isFullCharged && !previous.isFullCharged && previous.isCharging) {
            events += MiniEventType.FULLY_CHARGED
        }
        if (!current.isCharging && crossedDown(previous, current, lowThreshold)) {
            events += MiniEventType.LOW_BATTERY
        } else if (!current.isCharging && lowThreshold > CRITICAL_BATTERY_LEVEL &&
            crossedDown(previous, current, CRITICAL_BATTERY_LEVEL)
        ) {
            events += MiniEventType.LOW_BATTERY
        }
        return events
    }

    /** Battery reads only report changes in what we show; temperature jitter is ignored. */
    fun isVisibleChange(previous: ChargingState?, current: ChargingState): Boolean =
        previous == null ||
            previous.isCharging != current.isCharging ||
            previous.chargePercentage != current.chargePercentage ||
            previous.chargeType != current.chargeType ||
            previous.isFullCharged != current.isFullCharged ||
            kotlin.math.abs(previous.batteryTemperatureC - current.batteryTemperatureC) >= 1f

    private fun crossedDown(previous: ChargingState, current: ChargingState, level: Int): Boolean =
        previous.chargePercentage > level && current.chargePercentage <= level

    fun chargerLabel(type: ChargingType): String = when (type) {
        ChargingType.AC -> "Charging"
        ChargingType.USB -> "USB charging"
        ChargingType.WIRELESS -> "Wireless charging"
        ChargingType.DOCK -> "Dock charging"
        ChargingType.NONE -> "Charging"
    }
}
