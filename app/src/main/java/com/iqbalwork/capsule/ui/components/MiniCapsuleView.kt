package com.iqbalwork.capsule.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

/**
 * Compact collapsed pill view representing the idle/cutout island state.
 */
@Composable
fun MiniCapsuleView(
    state: CapsuleState,
    modifier: Modifier = Modifier
) {
    val miniEvent = state.miniEvent

    if (miniEvent != null) {
        // Mini Event Pop-up (TWS, Battery, Charging, etc.)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val eventColor = when (miniEvent.type) {
                MiniEventType.LOW_BATTERY -> CapsuleTokens.CalibratorRed
                MiniEventType.TWS_CONNECTED -> CapsuleTokens.Primary
                MiniEventType.CHARGER_CONNECTED -> Color(0xFF4CAF50)
                else -> CapsuleTokens.Primary
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(eventColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${miniEvent.title} ${miniEvent.subtitle}".trim(),
                color = CapsuleTokens.OnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        // Standard Idle Mini Capsule
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Indicator
            // Same rules as the media card: parked or hidden players leave the pill alone.
            val media = state.media?.takeIf { state.mediaVisible }
            val notification = state.notification
            if (media != null && media.isPlaying) {
                RotatingDiscIcon(artwork = media.artwork, dominantColor = Color(media.dominantColor))
            } else if (state.charging.isCharging) {
                ChargingIndicatorMini(percentage = state.charging.chargePercentage)
            } else if (notification != null) {
                NotificationDotMini(color = Color(notification.glowColor))
            } else {
                // Idle pill stays plain so it blends with the camera cutout.
                Spacer(modifier = Modifier.size(8.dp))
            }

            // Right Indicator
            val timer = state.timer
            if (media != null && media.isPlaying) {
                EqualizerWaveMini(color = mediaPalette(Color(media.dominantColor)).accent)
            } else if (timer != null && timer.isRunning) {
                val mins = timer.remainingSeconds / 60
                val secs = timer.remainingSeconds % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    color = CapsuleTokens.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (state.charging.isCharging) {
                Text(
                    text = "${state.charging.chargePercentage}%",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
fun RotatingDiscIcon(
    artwork: Bitmap?,
    dominantColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val discModifier = modifier
        .size(22.dp)
        .rotate(rotation)
        .clip(CircleShape)
    if (artwork != null) {
        Image(
            bitmap = remember(artwork) { artwork.asImageBitmap() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = discModifier
        )
    } else {
        Box(modifier = discModifier.background(dominantColor), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }
    }
}

@Composable
fun EqualizerWaveMini(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_wave")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Canvas(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val maxHeight = size.height

        val heights = listOf(bar1, bar2, bar3)
        heights.forEachIndexed { index, scale ->
            val h = maxHeight * scale
            val left = index * (barWidth + spacing)
            val top = maxHeight - h
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun ChargingIndicatorMini(percentage: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
    }
}

@Composable
fun NotificationDotMini(color: Color) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}
