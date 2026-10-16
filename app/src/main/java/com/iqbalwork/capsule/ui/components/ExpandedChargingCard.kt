package com.iqbalwork.capsule.ui.components

import com.iqbalwork.capsule.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.platform.system.BatteryEventDetector

/** F-30: level ring, charger type, time to full and battery temperature. */
@Composable
fun ExpandedChargingCard(
    chargingState: ChargingState,
    fahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (chargingState.isLowBattery) AlertRed else ChargingGreen
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Color.Black, color, 0.18f), Color.Black)))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            LevelRing(level = chargingState.chargePercentage, color = color, size = 96.dp, stroke = 8.dp)
            Text(
                text = "${chargingState.chargePercentage}%",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (chargingState.isCharging) {
                        stringResource(BatteryEventDetector.chargerLabelRes(chargingState.chargeType))
                    } else {
                        stringResource(R.string.st_on_battery)
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = when {
                    chargingState.isFullCharged -> stringResource(R.string.st_fully_charged)
                    chargingState.chargeTimeRemainingMs != null -> stringResource(R.string.st_full_in, formatDuration(chargingState.chargeTimeRemainingMs))
                    else -> stringResource(R.string.st_estimating)
                },
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatTemperature(chargingState.batteryTemperatureC, fahrenheit),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

fun formatTemperature(celsius: Float, fahrenheit: Boolean): String =
    if (fahrenheit) "%.1f °F".format(celsius * 9 / 5 + 32) else "%.1f °C".format(celsius)

@Composable
private fun formatDuration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(1)
    val hours = minutes / 60
    return if (hours > 0) {
        stringResource(R.string.st_duration_hm, hours, minutes % 60)
    } else {
        stringResource(R.string.st_duration_m, minutes)
    }
}
