package com.iqbalwork.capsule.platform.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType

/**
 * Battery level, charger and temperature from the sticky ACTION_BATTERY_CHANGED broadcast
 * (spec §2.5.3). Only changes the capsule shows are dispatched.
 */
class BatterySource(
    private val context: Context,
    private val settings: () -> Settings,
    private val dispatch: (SystemEvent) -> Unit
) {
    private val batteryManager = context.getSystemService(BatteryManager::class.java)
    private var last: ChargingState? = null
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = handle(intent)
    }

    fun start() {
        if (registered) return
        // The sticky intent is delivered to onReceive right away and becomes the baseline.
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
        last = null
    }

    private fun handle(intent: Intent) {
        val current = read(intent)
        val previous = last
        last = current
        if (!BatteryEventDetector.isVisibleChange(previous, current)) return
        dispatch(SystemEvent.ChargingUpdated(current))
        BatteryEventDetector.detect(previous, current, settings().lowBatteryThreshold).forEach { type ->
            dispatch(SystemEvent.MiniEventStarted(miniEvent(type, current)))
        }
    }

    private fun read(intent: Intent): ChargingState {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).takeIf { it > 0 } ?: 100
        val percent = if (level >= 0) (level * 100 / scale) else 0
        val type = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
            PLUGGED_DOCK -> ChargingType.DOCK
            else -> ChargingType.NONE
        }
        val charging = type != ChargingType.NONE
        val full = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL
        return ChargingState(
            isCharging = charging,
            chargePercentage = percent,
            chargeType = type,
            isFullCharged = full || (charging && percent >= 100),
            batteryTemperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            isLowBattery = !charging && percent <= settings().lowBatteryThreshold,
            chargeTimeRemainingMs = if (charging) batteryManager.computeChargeTimeRemaining().takeIf { it > 0 } else null
        )
    }

    private fun miniEvent(type: MiniEventType, state: ChargingState): MiniEvent {
        val percent = "${state.chargePercentage}%"
        val title = when (type) {
            MiniEventType.CHARGER_CONNECTED -> BatteryEventDetector.chargerLabel(state.chargeType)
            MiniEventType.CHARGER_DISCONNECTED -> "Charger removed"
            MiniEventType.FULLY_CHARGED -> "Fully charged"
            else -> "Low battery"
        }
        return MiniEvent(id = System.nanoTime(), type = type, title = title, subtitle = percent, level = state.chargePercentage)
    }

    private companion object {
        /** BatteryManager.BATTERY_PLUGGED_DOCK, public only from API 33. */
        const val PLUGGED_DOCK = 8
    }
}
