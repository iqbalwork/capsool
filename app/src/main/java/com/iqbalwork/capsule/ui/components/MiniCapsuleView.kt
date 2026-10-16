package com.iqbalwork.capsule.ui.components

import com.iqbalwork.capsule.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

/**
 * Compact collapsed pill view representing the idle/cutout island state.
 */
@Composable
fun MiniCapsuleView(
    state: CapsuleState,
    modifier: Modifier = Modifier,
    /** Width of the camera hole under the middle of the pill, in dp; 0 = none. */
    cameraGapDp: Int = 0,
    /** When above 0, a pop-up event leaves this much empty at the top (the camera hole) and draws below. */
    belowCameraTopDp: Int = 0,
    /** Show a pop-up event as just its icon (and a ring), keeping the pill the size it is when idle. */
    iconOnly: Boolean = false
) {
    val miniEvent = state.miniEvent

    if (miniEvent != null) {
        MiniEventView(
            event = miniEvent,
            modifier = modifier,
            style = state.settings.eventStyle(miniEvent.type.category),
            cameraGapDp = cameraGapDp,
            belowCameraTopDp = belowCameraTopDp,
            iconOnly = iconOnly
        )
    } else {
        // Compact pill: the left and right slots follow the card the carousel would open.
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val media = state.media
            val notification = state.topNotification
            val timer = state.timer
            when {
                state.activeCard == CapsuleCardType.NOTIFICATION && notification != null -> {
                    val accent = mediaPalette(Color(notification.glowColor)).accent
                    RoundBitmap(
                        bitmap = notification.avatar.takeIf { state.showsText(notification) },
                        fallbackText = if (state.showsText(notification)) notification.sender else notification.appName,
                        background = accent,
                        modifier = Modifier.size(24.dp)
                    )
                    notification.appIcon?.let { icon ->
                        Image(
                            bitmap = remember(icon) { icon.asImageBitmap() },
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accent),
                            modifier = Modifier.size(18.dp)
                        )
                    } ?: Spacer(Modifier.size(18.dp))
                }
                state.activeCard == CapsuleCardType.TIMER && timer != null -> {
                    val now by rememberTicker(timer.isRunning)
                    Icon(
                        if (timer.isCountdown) Icons.Rounded.Timer else Icons.Rounded.AvTimer,
                        contentDescription = null,
                        tint = TimerAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = timer.displayText(now),
                        color = if (timer.isRunning) TimerAmber else TimerAmber.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                    )
                }
                state.activeCard == CapsuleCardType.PROGRESS && state.progress.isNotEmpty() -> {
                    val item = state.progress.first()
                    val style = state.settings.progressStyle()
                    val accent = style.accent(item)
                    AppIconBadge(item, accent, size = 22, iconStyle = style.icon)
                    val fraction = item.fraction
                    if (fraction != null) {
                        LevelRing(level = (fraction * 100).toInt(), color = accent, size = 18.dp, stroke = 3.dp)
                    } else {
                        CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    }
                }
                state.activeCard == CapsuleCardType.MEDIA && media != null -> {
                    RotatingDiscIcon(artwork = media.artwork, dominantColor = Color(media.dominantColor))
                    if (media.isPlaying) {
                        EqualizerWaveMini(color = mediaPalette(Color(media.dominantColor)).accent)
                    } else {
                        Spacer(Modifier.size(8.dp))
                    }
                }
                state.activeCard == CapsuleCardType.CHARGING -> {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ChargingGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = "${state.charging.chargePercentage}%",
                        color = ChargingGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                state.activeCard == CapsuleCardType.SLIDERS && state.sliders.isTorchOn -> {
                    Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color(0xFFFFE082), modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.st_on), color = Color(0xFFFFE082), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                // Idle pill stays plain so it blends with the camera cutout.
                else -> Spacer(Modifier.size(8.dp))
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

