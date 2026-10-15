package com.iqbalwork.capsule.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedChargingCard(
    chargingState: ChargingState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "charging_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val chargingIcon = when (chargingState.chargeType) {
                    ChargingType.AC -> "⚡ AC Fast Charge"
                    ChargingType.USB -> "🔌 USB Cable"
                    ChargingType.WIRELESS -> "📶 Wireless Qi"
                    ChargingType.NONE -> if (chargingState.isFullCharged) "🔋 Fully Charged" else "🔋 Battery"
                }

                Text(
                    text = chargingIcon,
                    color = CapsuleTokens.OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Temperature Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CapsuleTokens.SurfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "🌡️ %.1f°C".format(chargingState.batteryTemperatureC),
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center Gauge / Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Battery Circle
            val chargeColor = when {
                chargingState.isLowBattery || chargingState.chargePercentage <= 5 -> CapsuleTokens.CalibratorRed
                chargingState.isCharging -> Color(0xFF4CAF50).copy(alpha = pulseAlpha)
                else -> CapsuleTokens.Primary
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(CapsuleTokens.SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${chargingState.chargePercentage}%",
                        color = chargeColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (chargingState.isCharging) "CHARGING" else "REMAINING",
                        color = CapsuleTokens.OnSurfaceVariant,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Horizontal battery bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(CapsuleTokens.SurfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (chargingState.chargePercentage / 100f).coerceIn(0f, 1f))
                            .background(chargeColor)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (chargingState.isLowBattery || chargingState.chargePercentage <= 5) {
                    Text(
                        text = "⚠️ Low Battery Warning! Connect charger now.",
                        color = CapsuleTokens.CalibratorRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (chargingState.isCharging) {
                    Text(
                        text = "Optimal power delivery active.",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = "Discharging normally.",
                        color = CapsuleTokens.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bottom status note
        Text(
            text = "Battery Health: Good • Voltage: 4.15V",
            color = CapsuleTokens.OnSurfaceVariant,
            fontSize = 10.sp
        )
    }
}
