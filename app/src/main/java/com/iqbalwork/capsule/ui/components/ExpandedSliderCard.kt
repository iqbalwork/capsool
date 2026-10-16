package com.iqbalwork.capsule.ui.components

import com.iqbalwork.capsule.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.SliderStyle
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.TorchState

/**
 * The Controls card (F-50..F-53): media volume with the ringer toggle, brightness with
 * auto-brightness, and the flashlight. Each row's icon is a toggle.
 */
@Composable
fun ExpandedSliderCard(
    sliders: SliderState,
    style: SliderStyle,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        ControlRow(
            icon = when (sliders.ringerMode) {
                RingerMode.NORMAL -> Icons.Rounded.NotificationsActive
                RingerMode.VIBRATE -> Icons.Rounded.Vibration
                RingerMode.SILENT -> Icons.Rounded.NotificationsOff
            },
            description = stringResource(R.string.st_ringer, stringResource(sliders.ringerMode.labelRes)),
            active = sliders.ringerMode != RingerMode.NORMAL,
            onIcon = { onAction(UserIntent.CycleRinger) }
        ) {
            StyledSlider(sliders.volumeLevel, style, onValueChange = { onAction(UserIntent.SetVolume(it)) })
        }

        ControlRow(
            icon = if (sliders.isAdaptiveBrightness) Icons.Rounded.BrightnessAuto else Icons.Rounded.BrightnessMedium,
            description = stringResource(R.string.st_auto_brightness),
            active = sliders.isAdaptiveBrightness,
            onIcon = { onAction(UserIntent.ToggleAdaptiveBrightness) }
        ) {
            if (sliders.canWriteBrightness) {
                StyledSlider(sliders.brightnessLevel, style, onValueChange = { onAction(UserIntent.SetBrightness(it)) })
            } else {
                Hint(stringResource(R.string.st_brightness_allow)) { onAction(UserIntent.SetBrightness(sliders.brightnessLevel)) }
            }
        }

        val torchUsable = sliders.torch == TorchState.ON || sliders.torch == TorchState.OFF
        ControlRow(
            icon = if (sliders.isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            description = stringResource(R.string.st_flashlight),
            active = sliders.isTorchOn,
            enabled = torchUsable,
            onIcon = { onAction(UserIntent.ToggleTorch) }
        ) {
            when {
                sliders.torch == TorchState.NONE -> Hint(stringResource(R.string.st_no_flashlight))
                sliders.torch == TorchState.BUSY -> Hint(stringResource(R.string.st_camera_busy))
                sliders.torchStrengthSupported -> StyledSlider(
                    value = if (sliders.isTorchOn) sliders.torchStrengthLevel else 0f,
                    style = style,
                    onValueChange = { onAction(UserIntent.SetTorchLevel(it)) }
                )
                else -> Hint(stringResource(if (sliders.isTorchOn) R.string.st_on else R.string.st_off))
            }
        }
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onIcon: () -> Unit,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) accent else Color.White.copy(alpha = if (enabled) 0.12f else 0.05f))
                .clickable(enabled = enabled, onClick = onIcon),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = when {
                    active -> MaterialTheme.colorScheme.onPrimary
                    enabled -> Color.White
                    else -> Color.White.copy(alpha = 0.35f)
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        content()
    }
}

@Composable
private fun RowScope.Hint(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text = text,
        color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .weight(1f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

/** F-51: three looks for the same 0..1 control. */
@Composable
private fun RowScope.StyledSlider(value: Float, style: SliderStyle, onValueChange: (Float) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    when (style) {
        SliderStyle.SPLIT -> {
            var local by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = local ?: value,
                onValueChange = {
                    local = it
                    onValueChange(it)
                },
                onValueChangeFinished = { local = null },
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                ),
                modifier = Modifier.weight(1f)
            )
        }
        SliderStyle.PILL -> DragSlider(value, onValueChange, Modifier.weight(1f)) { shown ->
            val radius = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(Color.White.copy(alpha = 0.14f), cornerRadius = radius)
            val fill = (size.width * shown).coerceAtLeast(size.height)
            drawRoundRect(accent, size = Size(fill, size.height), cornerRadius = radius)
        }
        SliderStyle.ROUNDED -> DragSlider(value, onValueChange, Modifier.weight(1f)) { shown ->
            val track = 8.dp.toPx()
            val thumb = 11.dp.toPx()
            val top = (size.height - track) / 2
            val usable = size.width - 2 * thumb
            val x = thumb + usable * shown
            drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(0f, top), Size(size.width, track), CornerRadius(track / 2))
            drawRoundRect(accent, Offset(0f, top), Size(x, track), CornerRadius(track / 2))
            drawCircle(Color.White, radius = thumb, center = Offset(x, size.height / 2))
        }
    }
}

/**
 * Tap or drag anywhere on the track. Pointer events are consumed, so dragging a slider is never
 * read as a swipe to the next card.
 */
@Composable
private fun DragSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    draw: DrawScope.(Float) -> Unit
) {
    var local by remember { mutableStateOf<Float?>(null) }
    val shown = (local ?: value).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun update(x: Float) {
                        val fraction = (x / size.width).coerceIn(0f, 1f)
                        local = fraction
                        onValueChange(fraction)
                    }
                    update(down.position.x)
                    down.consume()
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position.x)
                        change.consume()
                    }
                    local = null
                }
            }
    ) { draw(shown) }
}
