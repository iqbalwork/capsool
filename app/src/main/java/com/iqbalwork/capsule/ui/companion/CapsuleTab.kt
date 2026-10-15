package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.ColorSource
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MediaCollapseDelay
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.Settings
import kotlin.math.roundToInt

/** Geometry calibration, appearance and visibility rules (F-02, F-04, F-80, F-83, F-87). */
@Composable
fun CapsuleTab(
    settings: Settings,
    currentOrientation: Orientation,
    onUpdate: ((Settings) -> Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember(currentOrientation) { mutableStateOf(currentOrientation) }
    val geometry = settings.geometry(editing)
    fun updateGeometry(transform: (Geometry) -> Geometry) =
        onUpdate { it.withGeometry(editing, transform(it.geometry(editing))) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Section("Size and position") {
                SwitchRow(
                    title = "Calibration target",
                    description = "Move the capsule until the red target disappears under the camera.",
                    checked = settings.showCalibrationTarget,
                    onChange = { show -> onUpdate { it.copy(showCalibrationTarget = show) } }
                )
                Options(
                    options = Orientation.entries,
                    selected = editing,
                    label = { if (it == Orientation.PORTRAIT) "Portrait" else "Landscape" },
                    onSelect = { editing = it }
                )
                IntSlider("Horizontal offset", geometry.offsetXDp, -200..200) { v -> updateGeometry { it.copy(offsetXDp = v) } }
                IntSlider("Vertical offset", geometry.offsetYDp, -60..60) { v -> updateGeometry { it.copy(offsetYDp = v) } }
                IntSlider("Width", geometry.widthDp, 60..260) { v -> updateGeometry { it.copy(widthDp = v) } }
                IntSlider("Height", geometry.heightDp, 20..64) { v -> updateGeometry { it.copy(heightDp = v) } }
                IntSlider("Corner radius", geometry.cornerDp, 0..32) { v -> updateGeometry { it.copy(cornerDp = v) } }
                TextButton(onClick = { updateGeometry { Geometry() } }) { Text("Reset this orientation") }
            }
        }
        item {
            Section("Appearance") {
                SwitchRow(
                    title = "Always show pill",
                    description = "Keep the capsule visible even when nothing is happening.",
                    checked = settings.alwaysShowPill,
                    onChange = { show -> onUpdate { it.copy(alwaysShowPill = show) } }
                )
                Text("Idle opacity: ${(settings.idleOpacity * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.idleOpacity,
                    onValueChange = { v -> onUpdate { it.copy(idleOpacity = v) } },
                    steps = 4
                )
                Text("Accent colors", style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = ColorSource.entries,
                    selected = settings.colorSource,
                    label = { if (it == ColorSource.DYNAMIC) "From wallpaper" else "Capsool palette" },
                    onSelect = { source -> onUpdate { it.copy(colorSource = source) } }
                )
                SwitchRow(
                    title = "Haptics",
                    description = "Vibrate when a gesture on the capsule does something.",
                    checked = settings.hapticsEnabled,
                    onChange = { on -> onUpdate { it.copy(hapticsEnabled = on) } }
                )
            }
        }
        item {
            Section("Media") {
                SwitchRow(
                    title = "Show what is playing",
                    description = "Music, podcasts and videos from any player app.",
                    checked = settings.mediaEnabled,
                    onChange = { on -> onUpdate { it.copy(mediaEnabled = on) } }
                )
                SwitchRow(
                    title = "Hide while the player is open",
                    description = "No need for the card when the app is already on screen.",
                    checked = settings.hideMediaWhenAppInFront,
                    onChange = { on -> onUpdate { it.copy(hideMediaWhenAppInFront = on) } }
                )
                Text("After pausing, keep the card for", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaCollapseDelay.entries.forEach { delay ->
                        FilterChip(
                            selected = settings.mediaCollapseDelay == delay,
                            onClick = { onUpdate { it.copy(mediaCollapseDelay = delay) } },
                            label = { Text(delay.label) }
                        )
                    }
                }
            }
        }
        item {
            Section("Hide the capsule") {
                SwitchRow(
                    title = "In fullscreen apps",
                    description = "Videos, games and other apps that hide the status bar.",
                    checked = settings.hideInFullscreen,
                    onChange = { on -> onUpdate { it.copy(hideInFullscreen = on) } }
                )
                SwitchRow(
                    title = "While the keyboard is open",
                    description = null,
                    checked = settings.hideWithKeyboard,
                    onChange = { on -> onUpdate { it.copy(hideWithKeyboard = on) } }
                )
                SwitchRow(
                    title = "In landscape",
                    description = null,
                    checked = settings.hideInLandscape,
                    onChange = { on -> onUpdate { it.copy(hideInLandscape = on) } }
                )
            }
        }
    }
}

private val MediaCollapseDelay.label: String
    get() = when (this) {
        MediaCollapseDelay.INSTANT -> "No time"
        MediaCollapseDelay.SECONDS_5 -> "5 s"
        MediaCollapseDelay.SECONDS_15 -> "15 s"
        MediaCollapseDelay.SECONDS_30 -> "30 s"
        MediaCollapseDelay.SECONDS_60 -> "1 min"
        MediaCollapseDelay.MINUTES_2 -> "2 min"
        MediaCollapseDelay.NEVER -> "Always"
    }

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun IntSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Text("$label: $value dp", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat()
    )
}

@Composable
private fun <T> Options(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(label(option)) }
        }
    }
}
