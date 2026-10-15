package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.RingerMode
import com.iqbalwork.capsule.model.SliderState
import com.iqbalwork.capsule.model.VolumeSliderStyle
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedSliderCard(
    sliderState: SliderState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Volume Section with 3 Visual Styles ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔊 Volume (${(sliderState.volumeLevel * 100).toInt()}%)",
                        color = CapsuleTokens.OnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Style Switcher Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CapsuleTokens.SurfaceContainerHighest)
                            .clickable { onAction(UserIntent.CycleVolumeStyle) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        val styleBadge = when (sliderState.volumeStyle) {
                            VolumeSliderStyle.ANDROID_15 -> "A15 Pill"
                            VolumeSliderStyle.ANDROID_16 -> "A16 Split"
                            VolumeSliderStyle.SAMSUNG_ONE_UI -> "One UI"
                        }
                        Text(
                            text = styleBadge,
                            color = CapsuleTokens.Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Ringer Mode Toggle
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(CapsuleTokens.PrimaryContainer)
                            .clickable { onAction(UserIntent.CycleRinger) }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        val ringerIcon = when (sliderState.ringerMode) {
                            RingerMode.NORMAL -> "🔔 Ring"
                            RingerMode.VIBRATE -> "📳 Vib"
                            RingerMode.SILENT -> "🔕 Silent"
                        }
                        Text(
                            text = ringerIcon,
                            color = CapsuleTokens.OnPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Volume Visual Component
            when (sliderState.volumeStyle) {
                VolumeSliderStyle.ANDROID_15 -> {
                    // Android 15: Single Continuous Thick Pill
                    Android15VolumePill(
                        volume = sliderState.volumeLevel,
                        onVolumeChange = { onAction(UserIntent.SetVolume(it)) }
                    )
                }
                VolumeSliderStyle.ANDROID_16 -> {
                    // Android 16: Split Segmented Pills
                    Android16SegmentedPills(
                        volume = sliderState.volumeLevel,
                        onVolumeChange = { onAction(UserIntent.SetVolume(it)) }
                    )
                }
                VolumeSliderStyle.SAMSUNG_ONE_UI -> {
                    // Samsung One UI: Rounded Floating Slider
                    SamsungOneUISlider(
                        volume = sliderState.volumeLevel,
                        onVolumeChange = { onAction(UserIntent.SetVolume(it)) }
                    )
                }
            }
        }

        // --- 2. Brightness Section ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "☀️ Brightness (${(sliderState.brightnessLevel * 100).toInt()}%)",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (sliderState.isAdaptiveBrightness) "Adaptive: ON" else "Manual",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
            Slider(
                value = sliderState.brightnessLevel,
                onValueChange = { onAction(UserIntent.SetBrightness(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD54F),
                    activeTrackColor = Color(0xFFFFD54F),
                    inactiveTrackColor = CapsuleTokens.OutlineVariant
                )
            )
        }

        // --- 3. Flashlight Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CapsuleTokens.SurfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (sliderState.isTorchOn) CapsuleTokens.Primary else CapsuleTokens.SurfaceContainerHighest
                        )
                        .clickable { onAction(UserIntent.ToggleTorch) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sliderState.isTorchOn) "🔦" else "🔦",
                        color = if (sliderState.isTorchOn) CapsuleTokens.OnPrimary else CapsuleTokens.OnSurfaceVariant,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (sliderState.isTorchOn) "Torch ON" else "Torch OFF",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Strength slider
            Box(modifier = Modifier.width(140.dp)) {
                Slider(
                    value = sliderState.torchStrengthLevel,
                    onValueChange = { onAction(UserIntent.SetTorchLevel(it)) },
                    modifier = Modifier.height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CapsuleTokens.Primary,
                        activeTrackColor = CapsuleTokens.Primary,
                        inactiveTrackColor = CapsuleTokens.OutlineVariant
                    )
                )
            }
        }
    }
}

@Composable
fun Android15VolumePill(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(CapsuleTokens.SurfaceContainerHighest)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onVolumeChange(frac)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = volume)
                .background(CapsuleTokens.Primary)
        )
    }
}

@Composable
fun Android16SegmentedPills(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = 8
    val activeCount = (volume * segments).toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until segments) {
            val isActive = i < activeCount
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive) CapsuleTokens.Primary else CapsuleTokens.SurfaceContainerHighest
                    )
                    .clickable {
                        val newVol = (i + 1).toFloat() / segments.toFloat()
                        onVolumeChange(newVol)
                    }
            )
        }
    }
}

@Composable
fun SamsungOneUISlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = volume,
        onValueChange = onVolumeChange,
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        colors = SliderDefaults.colors(
            thumbColor = CapsuleTokens.Primary,
            activeTrackColor = CapsuleTokens.Primary,
            inactiveTrackColor = CapsuleTokens.SurfaceContainerHighest
        )
    )
}
