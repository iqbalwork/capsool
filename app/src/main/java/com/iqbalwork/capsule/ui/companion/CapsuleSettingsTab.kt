package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.preferences.CutoutPreferencesRepository
import com.iqbalwork.capsule.engine.state.CapsuleState
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.CutoutAlignment
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun CapsuleSettingsTab(
    state: CapsuleState,
    onAction: (UserAction) -> Unit,
    prefsRepo: CutoutPreferencesRepository? = null,
    modifier: Modifier = Modifier
) {
    val prefs = state.cutoutPreferences

    fun update(mutate: () -> com.iqbalwork.capsule.model.CutoutPreferences) {
        val newPrefs = mutate()
        onAction(UserAction.UpdateCutoutPreferences(newPrefs))
        prefsRepo?.updatePreferences(newPrefs)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Cutout & Display Calibration",
                color = CapsuleTokens.OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Calibrate the island around your device's camera hole punch and customize visual appearance.",
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // 1. Red Target Calibrator Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎯 Interactive Red Target Calibrator",
                        color = CapsuleTokens.CalibratorRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Displays a #FF3B30 target ring above camera cutout for easy physical centering.",
                        color = CapsuleTokens.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = prefs.showRedTarget,
                    onCheckedChange = { show ->
                        update { prefs.copy(showRedTarget = show) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CapsuleTokens.CalibratorRed,
                        checkedTrackColor = CapsuleTokens.SurfaceContainerHighest
                    )
                )
            }
        }

        // 2. Camera Cutout Alignment
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Camera Alignment",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CutoutAlignment.entries.forEach { align ->
                        val isSelected = prefs.alignment == align
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) CapsuleTokens.Primary else CapsuleTokens.SurfaceContainerHighest
                                )
                                .clickable {
                                    update { prefs.copy(alignment = align) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = align.name,
                                color = if (isSelected) CapsuleTokens.OnPrimary else CapsuleTokens.OnSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Capsule Dimensions Sliders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Capsule Size & Radius",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Width
                Text(
                    text = "Width: ${prefs.capsuleWidthDp} dp",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp
                )
                Slider(
                    value = prefs.capsuleWidthDp.toFloat(),
                    onValueChange = { w ->
                        update { prefs.copy(capsuleWidthDp = w.toInt()) }
                    },
                    valueRange = 80f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = CapsuleTokens.Primary,
                        activeTrackColor = CapsuleTokens.Primary
                    )
                )

                // Height
                Text(
                    text = "Height: ${prefs.capsuleHeightDp} dp",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp
                )
                Slider(
                    value = prefs.capsuleHeightDp.toFloat(),
                    onValueChange = { h ->
                        update { prefs.copy(capsuleHeightDp = h.toInt()) }
                    },
                    valueRange = 26f..56f,
                    colors = SliderDefaults.colors(
                        thumbColor = CapsuleTokens.Primary,
                        activeTrackColor = CapsuleTokens.Primary
                    )
                )

                // Corner Radius
                Text(
                    text = "Corner Radius: ${prefs.cornerRadiusDp} dp",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp
                )
                Slider(
                    value = prefs.cornerRadiusDp.toFloat(),
                    onValueChange = { r ->
                        update { prefs.copy(cornerRadiusDp = r.toInt()) }
                    },
                    valueRange = 10f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = CapsuleTokens.Primary,
                        activeTrackColor = CapsuleTokens.Primary
                    )
                )
            }
        }

        // 4. Idle Opacity Slider
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Idle Opacity",
                        color = CapsuleTokens.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(prefs.idleOpacity * 100).toInt()}%" + if (prefs.idleOpacity == 0f) " (OLED Safe)" else "",
                        color = CapsuleTokens.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = prefs.idleOpacity,
                    onValueChange = { op ->
                        update { prefs.copy(idleOpacity = op) }
                    },
                    valueRange = 0f..1.0f,
                    steps = 4, // 0%, 20%, 40%, 60%, 80%, 100%
                    colors = SliderDefaults.colors(
                        thumbColor = CapsuleTokens.Primary,
                        activeTrackColor = CapsuleTokens.Primary
                    )
                )

                Text(
                    text = "0% opacity completely hides the idle capsule to prevent OLED burn-in.",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}
