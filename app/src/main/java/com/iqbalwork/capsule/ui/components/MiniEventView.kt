package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.RowScope
import com.iqbalwork.capsule.core.settings.EventStyle
import com.iqbalwork.capsule.core.settings.EventRightShape
import com.iqbalwork.capsule.core.settings.EventLeftShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.AirplanemodeInactive
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HeadsetOff
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType

val ChargingGreen = Color(0xFF5BD37A)
val AlertRed = Color(0xFFFF6B5E)
private val AirplaneAmber = Color(0xFFFFB74D)
private val WifiBlue = Color(0xFF8AB4F8)

/**
 * The wide pill shown for a mini event (F-60..F-69): icon, title, and a value or battery ring.
 *
 * The pill is centered on the camera hole, which hides whatever is drawn over it. With a
 * [cameraGapDp] the content is therefore split in two equal halves with that strip left empty
 * between them: icon and title on the left, value and ring on the right.
 */
@Composable
fun MiniEventView(
    event: MiniEvent,
    modifier: Modifier = Modifier,
    style: EventStyle = EventStyle.Default,
    cameraGapDp: Int = 0
) {
    val color = style.color?.let { Color(it) } ?: event.type.accent()
    val split = cameraGapDp > 0

    @Composable
    fun Badge() {
        if (style.left == EventLeftShape.NONE) return
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(if (style.left == EventLeftShape.SQUARE) RoundedCornerShape(7.dp) else CircleShape)
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                style.icon?.let(::customIntentIcon) ?: event.type.icon(),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
    }

    @Composable
    fun RowScope.Title() {
        Text(
            text = event.title,
            color = Color.White,
            style = if (split) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }

    @Composable
    fun Trailing() {
        if (event.subtitle.isNotEmpty() && style.right != EventRightShape.NONE) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = event.subtitle,
                color = if (event.level != null) color else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        event.level?.takeIf { style.right == EventRightShape.AUTO }?.let { level ->
            Spacer(Modifier.width(6.dp))
            LevelRing(level = level, color = color, size = 18.dp, stroke = 3.dp)
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (split) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Badge()
                Title()
            }
            Spacer(Modifier.width(cameraGapDp.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) { Trailing() }
        } else {
            Badge()
            Title()
            Trailing()
        }
    }
}

/** A thin progress ring, used for battery levels. */
@Composable
fun LevelRing(level: Int, color: Color, size: Dp, stroke: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = stroke.toPx()
        val style = Stroke(width = strokePx, cap = StrokeCap.Round)
        drawArc(color.copy(alpha = 0.25f), 0f, 360f, useCenter = false, style = style)
        drawArc(color, -90f, 360f * level.coerceIn(0, 100) / 100f, useCenter = false, style = style)
    }
}

fun MiniEventType.icon(): ImageVector = when (this) {
    MiniEventType.CHARGER_CONNECTED -> Icons.Rounded.Bolt
    MiniEventType.CHARGER_DISCONNECTED -> Icons.Rounded.PowerOff
    MiniEventType.FULLY_CHARGED -> Icons.Rounded.BatteryFull
    MiniEventType.LOW_BATTERY -> Icons.Rounded.BatteryAlert
    MiniEventType.RINGER_NORMAL -> Icons.Rounded.NotificationsActive
    MiniEventType.RINGER_VIBRATE -> Icons.Rounded.Vibration
    MiniEventType.RINGER_SILENT -> Icons.Rounded.NotificationsOff
    MiniEventType.HEADPHONES_CONNECTED -> Icons.Rounded.Headphones
    MiniEventType.HEADPHONES_DISCONNECTED -> Icons.Rounded.HeadsetOff
    MiniEventType.WIFI_CONNECTED -> Icons.Rounded.Wifi
    MiniEventType.WIFI_DISCONNECTED -> Icons.Rounded.WifiOff
    MiniEventType.AIRPLANE_ON -> Icons.Rounded.AirplanemodeActive
    MiniEventType.AIRPLANE_OFF -> Icons.Rounded.AirplanemodeInactive
    MiniEventType.DEVICE_UNLOCKED -> Icons.Rounded.LockOpen
    MiniEventType.AUTO_BRIGHTNESS_ON -> Icons.Rounded.BrightnessAuto
    MiniEventType.AUTO_BRIGHTNESS_OFF -> Icons.Rounded.BrightnessMedium
    MiniEventType.PROGRESS_DONE -> Icons.Rounded.DownloadDone
    MiniEventType.ACTION_FAILED -> Icons.Rounded.ErrorOutline
}

@Composable
private fun MiniEventType.accent(): Color = when (this) {
    MiniEventType.CHARGER_CONNECTED, MiniEventType.FULLY_CHARGED, MiniEventType.PROGRESS_DONE -> ChargingGreen
    MiniEventType.LOW_BATTERY, MiniEventType.ACTION_FAILED -> AlertRed
    MiniEventType.AIRPLANE_ON, MiniEventType.AIRPLANE_OFF -> AirplaneAmber
    MiniEventType.WIFI_CONNECTED, MiniEventType.WIFI_DISCONNECTED -> WifiBlue
    MiniEventType.CHARGER_DISCONNECTED -> Color.White
    else -> MaterialTheme.colorScheme.primary
}
