package com.iqbalwork.capsule.ui.companion

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.ColorSource
import com.iqbalwork.capsule.core.settings.DEFAULT_BRIGHTNESS_MAX
import com.iqbalwork.capsule.core.settings.SliderStyle
import com.iqbalwork.capsule.core.settings.Geometry
import com.iqbalwork.capsule.core.settings.MediaCollapseDelay
import com.iqbalwork.capsule.core.settings.MiniEventDuration
import com.iqbalwork.capsule.model.EventCategory
import com.iqbalwork.capsule.core.settings.NotificationDelay
import com.iqbalwork.capsule.core.settings.NotificationDisplay
import com.iqbalwork.capsule.core.settings.Orientation
import com.iqbalwork.capsule.core.settings.LockScreenBehavior
import com.iqbalwork.capsule.core.settings.Settings
import kotlin.math.roundToInt

/** Geometry calibration, appearance and visibility rules (F-02, F-04, F-80, F-83, F-87). */
@Composable
fun CapsuleTab(
    settings: Settings,
    currentOrientation: Orientation,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onOpenNotificationApps: () -> Unit,
    onBluetoothGranted: () -> Unit,
    onControlsPermissionsChanged: () -> Unit,
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
            Section("Notifications") {
                SwitchRow(
                    title = "Show notifications",
                    description = "Messages and alerts from your apps appear in the capsule.",
                    checked = settings.notificationsEnabled,
                    onChange = { on -> onUpdate { it.copy(notificationsEnabled = on) } }
                )
                Text("Notification style", style = MaterialTheme.typography.bodyLarge)
                Options(
                    options = NotificationDisplay.entries,
                    selected = settings.notificationDisplay,
                    label = { if (it == NotificationDisplay.COMPACT) "Compact" else "Expanded" },
                    onSelect = { display -> onUpdate { it.copy(notificationDisplay = display) } }
                )
                Text(
                    text = if (settings.notificationDisplay == NotificationDisplay.COMPACT) {
                        "New notifications appear as a small pill with the sender's photo. Tap it to read and reply."
                    } else {
                        "New notifications open the full card right away, with the message and reply buttons."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Keep it in the capsule for", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationDelay.entries.forEach { delay ->
                        FilterChip(
                            selected = settings.notificationDelay == delay,
                            onClick = { onUpdate { it.copy(notificationDelay = delay) } },
                            label = { Text(delay.label) }
                        )
                    }
                }
                Text("On the lock screen", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockScreenBehavior.entries.forEach { behavior ->
                        FilterChip(
                            selected = settings.lockScreenBehavior == behavior,
                            onClick = { onUpdate { it.copy(lockScreenBehavior = behavior) } },
                            label = { Text(behavior.displayName) }
                        )
                    }
                }
                Text(
                    when (settings.lockScreenBehavior) {
                        LockScreenBehavior.SHOW ->
                            "The capsule shows, message text included."
                        LockScreenBehavior.SHOW_WITHOUT_CONTENT ->
                            "The capsule shows, but only the app name — unless the app marks the message public."
                        LockScreenBehavior.HIDE ->
                            "The capsule stays away until you unlock."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SwitchRow(
                    title = "Remove after viewing",
                    description = "Clear it from the notification shade once you have opened it in the capsule.",
                    checked = settings.removeNotificationAfterViewing,
                    onChange = { on -> onUpdate { it.copy(removeNotificationAfterViewing = on) } }
                )
                SwitchRow(
                    title = "Glow",
                    description = "Outline the capsule in the app's color while a notification waits.",
                    checked = settings.notificationGlow,
                    onChange = { on -> onUpdate { it.copy(notificationGlow = on) } }
                )
                TextButton(onClick = onOpenNotificationApps) { Text("Choose apps and turn off pop-ups") }
            }
        }
        item {
            ControlsSection(settings = settings, onUpdate = onUpdate, onPermissionsChanged = onControlsPermissionsChanged)
        }
        item {
            EventsSection(settings = settings, onUpdate = onUpdate, onBluetoothGranted = onBluetoothGranted)
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
                HiddenAppsList(settings = settings, onUpdate = onUpdate)
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

/** F-51..F-53: slider look, brightness calibration and the permissions the controls need. */
@Composable
private fun ControlsSection(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onPermissionsChanged: () -> Unit
) {
    val context = LocalContext.current
    var canWrite by remember { mutableStateOf(false) }
    var canSilence by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        canWrite = AndroidSettings.System.canWrite(context)
        canSilence = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        onPermissionsChanged() // the user may have just granted something in Settings
        onPauseOrDispose { }
    }
    Section("Controls") {
        Text(
            "Tap the capsule when nothing is happening, or long-press it, to open volume, brightness and flashlight.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Slider style", style = MaterialTheme.typography.bodyMedium)
        Options(
            options = SliderStyle.entries,
            selected = settings.sliderStyle,
            label = {
                when (it) {
                    SliderStyle.PILL -> "Pill"
                    SliderStyle.SPLIT -> "Split"
                    SliderStyle.ROUNDED -> "Rounded"
                }
            },
            onSelect = { style -> onUpdate { it.copy(sliderStyle = style) } }
        )
        PermissionRow(
            title = "Brightness control",
            granted = canWrite,
            onGrant = {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
        )
        PermissionRow(
            title = "Silent mode",
            granted = canSilence,
            onGrant = { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        )
        Text("Brightness calibration", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Android 13+ does not tell apps the highest brightness value. Drag the system brightness slider " +
                "all the way up, then tap Calibrate. Current maximum: ${settings.brightnessMax}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val current = AndroidSettings.System.getInt(context.contentResolver, AndroidSettings.System.SCREEN_BRIGHTNESS, 0)
                if (current > 0) onUpdate { it.copy(brightnessMax = current) }
            }) { Text("Calibrate") }
            TextButton(onClick = { onUpdate { it.copy(brightnessMax = DEFAULT_BRIGHTNESS_MAX) } }) { Text("Reset") }
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) "Allowed" else "Not allowed yet",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        if (!granted) TextButton(onClick = onGrant) { Text("Allow") }
    }
}

@Composable
private fun EventsSection(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBluetoothGranted: () -> Unit
) {
    val context = LocalContext.current
    var bluetoothAllowed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        bluetoothAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        onPauseOrDispose { }
    }
    val requestBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        bluetoothAllowed = granted
        if (granted) onBluetoothGranted()
    }
    fun toggle(category: EventCategory, on: Boolean) = onUpdate {
        it.copy(enabledEvents = if (on) it.enabledEvents + category else it.enabledEvents - category)
    }

    Section("Events") {
        SwitchRow(
            title = "Charging card",
            description = "Show the battery level in the capsule while charging.",
            checked = settings.chargingCardEnabled,
            onChange = { on -> onUpdate { it.copy(chargingCardEnabled = on) } }
        )
        SwitchRow(
            title = "Downloads and uploads",
            description = "Progress from any app that shows a progress bar in its notification.",
            checked = settings.progressEnabled,
            onChange = { on -> onUpdate { it.copy(progressEnabled = on) } }
        )
        SwitchRow(
            title = "Timers and stopwatches",
            description = "From your clock app, with its own buttons.",
            checked = settings.timerEnabled,
            onChange = { on -> onUpdate { it.copy(timerEnabled = on) } }
        )
        Text("Pop-ups for", style = MaterialTheme.typography.bodyLarge)
        EventCategory.entries.forEach { category ->
            SwitchRow(
                title = category.label,
                description = category.description,
                checked = category in settings.enabledEvents,
                onChange = { on -> toggle(category, on) }
            )
            if (category == EventCategory.HEADPHONES && category in settings.enabledEvents && !bluetoothAllowed) {
                TextButton(onClick = { requestBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                    Text("Allow Bluetooth access to see headphones")
                }
            }
        }
        Text("Show each pop-up for", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniEventDuration.entries.forEach { duration ->
                FilterChip(
                    selected = settings.miniEventDuration == duration,
                    onClick = { onUpdate { it.copy(miniEventDuration = duration) } },
                    label = { Text("${duration.millis / 1000} s") }
                )
            }
        }
        IntSlider("Low battery warning at", settings.lowBatteryThreshold, 5..30, unit = "%") { v ->
            onUpdate { it.copy(lowBatteryThreshold = v) }
        }
        SwitchRow(
            title = "Temperature in °F",
            description = null,
            checked = settings.temperatureFahrenheit,
            onChange = { on -> onUpdate { it.copy(temperatureFahrenheit = on) } }
        )
    }
}

private val EventCategory.label: String
    get() = when (this) {
        EventCategory.CHARGING -> "Charger plugged in or out"
        EventCategory.LOW_BATTERY -> "Low battery"
        EventCategory.RINGER -> "Ring, vibrate, silent"
        EventCategory.HEADPHONES -> "Bluetooth headphones"
        EventCategory.WIFI -> "Wi-Fi connected or lost"
        EventCategory.AIRPLANE -> "Airplane mode"
        EventCategory.UNLOCK -> "Unlocking the phone"
        EventCategory.BRIGHTNESS_MODE -> "Auto-brightness"
        EventCategory.DOWNLOADS -> "Download finished"
    }

private val EventCategory.description: String?
    get() = when (this) {
        EventCategory.HEADPHONES -> "Shows the battery too, if the headphones report it."
        EventCategory.LOW_BATTERY -> "At your threshold and again at 5%."
        else -> null
    }

private val NotificationDelay.label: String
    get() = when (this) {
        NotificationDelay.SECONDS_1 -> "1 s"
        NotificationDelay.SECONDS_3 -> "3 s"
        NotificationDelay.SECONDS_5 -> "5 s"
        NotificationDelay.SECONDS_10 -> "10 s"
        NotificationDelay.SECONDS_20 -> "20 s"
        NotificationDelay.SECONDS_30 -> "30 s"
        NotificationDelay.UNTIL_OPENED -> "Until opened"
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

/** F-84: apps where the capsule stays out of the way. Lives inside the "Hide the capsule" section. */
@Composable
private fun HiddenAppsList(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    val context = LocalContext.current
    var pickingApp by remember { mutableStateOf(false) }
    val packageManager = context.packageManager
    // Only package names are stored — that is what the visibility rule compares. Labels are
    // resolved for display so the list reads like app names instead of package strings, and an
    // unresolvable package falls back to its own name rather than vanishing.
    val labels = remember(settings.hiddenPackages) {
        settings.hiddenPackages.associateWith { packageName ->
            runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
                ?.let { packageManager.getApplicationLabel(it).toString() }
                ?: packageName
        }
    }

    Column {
        Text("Stay hidden in these apps", style = MaterialTheme.typography.bodyLarge)
        Text(
            "The capsule hides while one of these apps is in front, and comes back when you leave.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        settings.hiddenPackages
            .sortedBy { labels[it] ?: it }
            .forEach { packageName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        labels[packageName] ?: packageName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            onUpdate { it.copy(hiddenPackages = it.hiddenPackages - packageName) }
                        }
                    ) { Text("Remove") }
                }
            }
        TextButton(onClick = { pickingApp = true }) { Text("Add an app") }
    }

    if (pickingApp) {
        AppPickerDialog(
            title = "Keep the capsule out of which app?",
            onPick = { app ->
                onUpdate { it.copy(hiddenPackages = it.hiddenPackages + app.packageName) }
                pickingApp = false
            },
            onDismiss = { pickingApp = false }
        )
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
private fun IntSlider(label: String, value: Int, range: IntRange, unit: String = "dp", onChange: (Int) -> Unit) {
    Text("$label: $value $unit".replace(" %", "%"), style = MaterialTheme.typography.bodyMedium)
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
