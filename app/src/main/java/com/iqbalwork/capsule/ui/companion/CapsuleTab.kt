package com.iqbalwork.capsule.ui.companion

import android.Manifest
import com.iqbalwork.capsule.core.settings.MAX_CARD_CORNER_DP
import com.iqbalwork.capsule.core.settings.MIN_CARD_CORNER_DP
import com.iqbalwork.capsule.core.settings.CardWidthMode
import com.iqbalwork.capsule.core.settings.PlacementMode
import com.iqbalwork.capsule.platform.ScreenSize
import com.iqbalwork.capsule.core.settings.ScreenProfileKey
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.settings.BounceLevel
import com.iqbalwork.capsule.core.settings.CapsuleShapeStyle
import com.iqbalwork.capsule.core.settings.ColorSource
import com.iqbalwork.capsule.core.settings.DefaultCardPriority
import com.iqbalwork.capsule.core.settings.ReorderableCards
import com.iqbalwork.capsule.core.settings.moveCard
import com.iqbalwork.capsule.core.settings.normalizeCardPriority
import com.iqbalwork.capsule.core.settings.ExpandStyle
import com.iqbalwork.capsule.core.settings.MAX_ANIMATION_SPEED
import com.iqbalwork.capsule.core.settings.MAX_BORDER_DP
import com.iqbalwork.capsule.core.settings.MAX_SHADOW_DP
import com.iqbalwork.capsule.core.settings.MIN_ANIMATION_SPEED
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
    // F-08: the calibration belongs to this panel and the orientation being edited.
    val context = LocalContext.current
    val profile = remember(editing) {
        val (width, height) = ScreenSize.of(context)
        ScreenProfileKey.of(width, height, editing)
    }
    val geometry = settings.geometry(profile, editing)
    fun updateGeometry(transform: (Geometry) -> Geometry) =
        onUpdate { it.withGeometry(profile, editing, transform(it.geometry(profile, editing))) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Section(stringResource(R.string.cap_sec_size_position)) {
                // The red target hides under the camera hole; independent mode is not tied to the camera.
                if (settings.placementMode == PlacementMode.CAMERA) {
                    SwitchRow(
                        title = stringResource(R.string.cap_calibration_target),
                        description = stringResource(R.string.cap_calibration_target_desc),
                        checked = settings.showCalibrationTarget,
                        onChange = { show -> onUpdate { it.copy(showCalibrationTarget = show) } }
                    )
                }
                Options(
                    options = Orientation.entries,
                    selected = editing,
                    label = { stringResource(if (it == Orientation.PORTRAIT) R.string.cap_portrait else R.string.cap_landscape) },
                    onSelect = { editing = it }
                )
                IntSlider(stringResource(R.string.cap_offset_x), geometry.offsetXDp, -200..200) { v -> updateGeometry { it.copy(offsetXDp = v) } }
                IntSlider(stringResource(R.string.cap_offset_y), geometry.offsetYDp, -60..60) { v -> updateGeometry { it.copy(offsetYDp = v) } }
                IntSlider(stringResource(R.string.cap_width), geometry.widthDp, 60..260) { v -> updateGeometry { it.copy(widthDp = v) } }
                IntSlider(stringResource(R.string.cap_height), geometry.heightDp, 20..64) { v -> updateGeometry { it.copy(heightDp = v) } }
                IntSlider(stringResource(R.string.cap_corner_radius), geometry.cornerDp, 0..32) { v -> updateGeometry { it.copy(cornerDp = v) } }
                profile?.let { key ->
                    val calibrated = key in settings.profiles
                    Text(
                        stringResource(
                            if (calibrated) R.string.prof_info_own else R.string.prof_info_template,
                            key.shortPx, key.longPx
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { updateGeometry { Geometry() } }) { Text(stringResource(R.string.cap_reset_orientation)) }
            }
        }
        item {
            Section(stringResource(R.string.mode_section)) {
                Text(stringResource(R.string.mode_placement), style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = PlacementMode.entries,
                    selected = settings.placementMode,
                    label = {
                        stringResource(if (it == PlacementMode.CAMERA) R.string.mode_camera else R.string.mode_independent)
                    },
                    onSelect = { mode -> onUpdate { it.copy(placementMode = mode) } }
                )
                Text(
                    stringResource(
                        if (settings.placementMode == PlacementMode.CAMERA) R.string.mode_camera_desc
                        else R.string.mode_independent_desc
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stringResource(R.string.mode_card_width), style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = CardWidthMode.entries,
                    selected = settings.cardWidthMode,
                    label = {
                        stringResource(if (it == CardWidthMode.CONTENT) R.string.mode_width_content else R.string.mode_width_full)
                    },
                    onSelect = { mode -> onUpdate { it.copy(cardWidthMode = mode) } }
                )
                IntSlider(
                    stringResource(R.string.mode_card_radius),
                    settings.cardCornerDp,
                    MIN_CARD_CORNER_DP..MAX_CARD_CORNER_DP
                ) { v -> onUpdate { it.copy(cardCornerDp = v) } }
            }
        }
        item {
            Section(stringResource(R.string.cap_sec_appearance)) {
                SwitchRow(
                    title = stringResource(R.string.cap_always_show_pill),
                    description = stringResource(R.string.cap_always_show_pill_desc),
                    checked = settings.alwaysShowPill,
                    onChange = { show -> onUpdate { it.copy(alwaysShowPill = show) } }
                )
                val idleOpacityLabel = stringResource(R.string.cap_idle_opacity, (settings.idleOpacity * 100).roundToInt())
                Text(idleOpacityLabel, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.idleOpacity,
                    onValueChange = { v -> onUpdate { it.copy(idleOpacity = v) } },
                    modifier = Modifier.semantics {
                        contentDescription = idleOpacityLabel
                    },
                    steps = 4
                )
                Text(stringResource(R.string.cap_accent_colors), style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = ColorSource.entries,
                    selected = settings.colorSource,
                    label = { stringResource(if (it == ColorSource.DYNAMIC) R.string.cap_from_wallpaper else R.string.cap_capsool_palette) },
                    onSelect = { source -> onUpdate { it.copy(colorSource = source) } }
                )
                SwitchRow(
                    title = stringResource(R.string.cap_haptics),
                    description = stringResource(R.string.cap_haptics_desc),
                    checked = settings.hapticsEnabled,
                    onChange = { on -> onUpdate { it.copy(hapticsEnabled = on) } }
                )
            }
        }
        item {
            Section(stringResource(R.string.cap_sec_card_priority)) {
                Text(
                    stringResource(R.string.cap_card_priority_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val order = normalizeCardPriority(settings.cardPriority).filter { it in ReorderableCards }
                order.forEachIndexed { index, card ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.cap_card_priority_item, index + 1, stringResource(card.labelRes)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = index > 0,
                            onClick = { onUpdate { it.copy(cardPriority = moveCard(it.cardPriority, card, -1)) } }
                        ) { Text(stringResource(R.string.cap_up)) }
                        TextButton(
                            enabled = index < order.lastIndex,
                            onClick = { onUpdate { it.copy(cardPriority = moveCard(it.cardPriority, card, 1)) } }
                        ) { Text(stringResource(R.string.cap_down)) }
                    }
                }
                TextButton(onClick = { onUpdate { it.copy(cardPriority = DefaultCardPriority) } }) {
                    Text(stringResource(R.string.cap_reset_order))
                }
            }
        }
        item {
            Section(stringResource(R.string.cap_sec_shape_colors)) {
                Text(stringResource(R.string.cap_shape), style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = CapsuleShapeStyle.entries,
                    selected = settings.shapeStyle,
                    label = {
                        when (it) {
                            CapsuleShapeStyle.PILL -> stringResource(R.string.cap_shape_pill)
                            CapsuleShapeStyle.ROUNDED_RECT -> stringResource(R.string.cap_shape_rounded)
                            CapsuleShapeStyle.SQUIRCLE -> stringResource(R.string.cap_shape_squircle)
                        }
                    },
                    onSelect = { style -> onUpdate { it.copy(shapeStyle = style) } }
                )
                if (settings.shapeStyle == CapsuleShapeStyle.PILL) {
                    Text(
                        stringResource(R.string.cap_pill_round_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(stringResource(R.string.cap_background), style = MaterialTheme.typography.bodyMedium)
                SwatchRow(
                    colors = BackgroundSwatches,
                    selected = settings.customBackground,
                    defaultLabel = stringResource(R.string.cap_black),
                    onSelect = { color -> onUpdate { it.copy(customBackground = color) } }
                )
                Text(
                    stringResource(R.string.cap_background_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IntSlider(stringResource(R.string.cap_border), settings.borderDp, 0..MAX_BORDER_DP) { v -> onUpdate { it.copy(borderDp = v) } }
                if (settings.borderDp > 0) {
                    SwatchRow(
                        colors = BorderSwatches,
                        selected = settings.borderColor,
                        defaultLabel = stringResource(R.string.cap_theme),
                        onSelect = { color -> onUpdate { it.copy(borderColor = color) } }
                    )
                }
                IntSlider(stringResource(R.string.cap_shadow), settings.shadowDp, 0..MAX_SHADOW_DP) { v -> onUpdate { it.copy(shadowDp = v) } }
                TextButton(onClick = {
                    onUpdate {
                        it.copy(
                            shapeStyle = CapsuleShapeStyle.ROUNDED_RECT,
                            customBackground = null,
                            borderDp = 0,
                            borderColor = null,
                            shadowDp = 0
                        )
                    }
                }) { Text(stringResource(R.string.cap_reset_shape_colors)) }
            }
        }
        item {
            Section(stringResource(R.string.cap_sec_animation)) {
                val speedLabel = stringResource(R.string.cap_speed, "%.1f".format(settings.animationSpeed))
                Text(speedLabel, style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.animationSpeed,
                    onValueChange = { v -> onUpdate { it.copy(animationSpeed = (v * 10).roundToInt() / 10f) } },
                    valueRange = MIN_ANIMATION_SPEED..MAX_ANIMATION_SPEED,
                    modifier = Modifier.semantics { contentDescription = speedLabel },
                    steps = 14,
                    enabled = !settings.reduceAnimations
                )
                Text(stringResource(R.string.cap_expand_style), style = MaterialTheme.typography.bodyMedium)
                Options(
                    options = ExpandStyle.entries,
                    selected = settings.expandStyle,
                    label = { stringResource(if (it == ExpandStyle.ORGANIC) R.string.cap_organic else R.string.cap_rigid) },
                    onSelect = { style -> onUpdate { it.copy(expandStyle = style) } }
                )
                if (settings.expandStyle == ExpandStyle.ORGANIC && !settings.reduceAnimations) {
                    Text(stringResource(R.string.cap_bounce), style = MaterialTheme.typography.bodyMedium)
                    Options(
                        options = BounceLevel.entries,
                        selected = settings.bounce,
                        label = {
                            when (it) {
                                BounceLevel.NONE -> stringResource(R.string.cap_bounce_none)
                                BounceLevel.LOW -> stringResource(R.string.cap_bounce_low)
                                BounceLevel.DEFAULT -> stringResource(R.string.cap_bounce_normal)
                                BounceLevel.HIGH -> stringResource(R.string.cap_bounce_high)
                                BounceLevel.MAX -> stringResource(R.string.cap_bounce_max)
                            }
                        },
                        onSelect = { level -> onUpdate { it.copy(bounce = level) } }
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.cap_reduce_animations),
                    description = stringResource(R.string.cap_reduce_animations_desc),
                    checked = settings.reduceAnimations,
                    onChange = { on -> onUpdate { it.copy(reduceAnimations = on) } }
                )
            }
        }
        item {
            Section(stringResource(R.string.cap_sec_media)) {
                SwitchRow(
                    title = stringResource(R.string.cap_media_show),
                    description = stringResource(R.string.cap_media_show_desc),
                    checked = settings.mediaEnabled,
                    onChange = { on -> onUpdate { it.copy(mediaEnabled = on) } }
                )
                SwitchRow(
                    title = stringResource(R.string.cap_media_hide_open),
                    description = stringResource(R.string.cap_media_hide_open_desc),
                    checked = settings.hideMediaWhenAppInFront,
                    onChange = { on -> onUpdate { it.copy(hideMediaWhenAppInFront = on) } }
                )
                Text(stringResource(R.string.cap_media_keep_for), style = MaterialTheme.typography.bodyMedium)
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
            Section(stringResource(R.string.cap_sec_notifications)) {
                SwitchRow(
                    title = stringResource(R.string.cap_notif_show),
                    description = stringResource(R.string.cap_notif_show_desc),
                    checked = settings.notificationsEnabled,
                    onChange = { on -> onUpdate { it.copy(notificationsEnabled = on) } }
                )
                Text(stringResource(R.string.cap_notif_style), style = MaterialTheme.typography.bodyLarge)
                Options(
                    options = NotificationDisplay.entries,
                    selected = settings.notificationDisplay,
                    label = { stringResource(if (it == NotificationDisplay.COMPACT) R.string.cap_notif_compact else R.string.cap_notif_expanded) },
                    onSelect = { display -> onUpdate { it.copy(notificationDisplay = display) } }
                )
                Text(
                    text = if (settings.notificationDisplay == NotificationDisplay.COMPACT) {
                        stringResource(R.string.cap_notif_compact_desc)
                    } else {
                        stringResource(R.string.cap_notif_expanded_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stringResource(R.string.cap_notif_keep_for), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationDelay.entries.forEach { delay ->
                        FilterChip(
                            selected = settings.notificationDelay == delay,
                            onClick = { onUpdate { it.copy(notificationDelay = delay) } },
                            label = { Text(delay.label) }
                        )
                    }
                }
                Text(stringResource(R.string.cap_lock_screen), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LockScreenBehavior.entries.forEach { behavior ->
                        FilterChip(
                            selected = settings.lockScreenBehavior == behavior,
                            onClick = { onUpdate { it.copy(lockScreenBehavior = behavior) } },
                            label = { Text(stringResource(behavior.labelRes)) }
                        )
                    }
                }
                Text(
                    when (settings.lockScreenBehavior) {
                        LockScreenBehavior.SHOW ->
                            stringResource(R.string.cap_lock_show_desc)
                        LockScreenBehavior.SHOW_WITHOUT_CONTENT ->
                            stringResource(R.string.cap_lock_without_content_desc)
                        LockScreenBehavior.HIDE ->
                            stringResource(R.string.cap_lock_hide_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SwitchRow(
                    title = stringResource(R.string.cap_remove_after_viewing),
                    description = stringResource(R.string.cap_remove_after_viewing_desc),
                    checked = settings.removeNotificationAfterViewing,
                    onChange = { on -> onUpdate { it.copy(removeNotificationAfterViewing = on) } }
                )
                SwitchRow(
                    title = stringResource(R.string.cap_glow),
                    description = stringResource(R.string.cap_glow_desc),
                    checked = settings.notificationGlow,
                    onChange = { on -> onUpdate { it.copy(notificationGlow = on) } }
                )
                TextButton(onClick = onOpenNotificationApps) { Text(stringResource(R.string.cap_choose_apps)) }
            }
        }
        item {
            ControlsSection(settings = settings, onUpdate = onUpdate, onPermissionsChanged = onControlsPermissionsChanged)
        }
        item {
            EventsSection(settings = settings, onUpdate = onUpdate, onBluetoothGranted = onBluetoothGranted)
        }
        item {
            Section(stringResource(R.string.cap_sec_hide)) {
                SwitchRow(
                    title = stringResource(R.string.cap_hide_fullscreen),
                    description = stringResource(R.string.cap_hide_fullscreen_desc),
                    checked = settings.hideInFullscreen,
                    onChange = { on -> onUpdate { it.copy(hideInFullscreen = on) } }
                )
                SwitchRow(
                    title = stringResource(R.string.cap_hide_keyboard),
                    description = null,
                    checked = settings.hideWithKeyboard,
                    onChange = { on -> onUpdate { it.copy(hideWithKeyboard = on) } }
                )
                SwitchRow(
                    title = stringResource(R.string.cap_hide_landscape),
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
    @Composable get() = when (this) {
        MediaCollapseDelay.INSTANT -> stringResource(R.string.cap_delay_none)
        MediaCollapseDelay.SECONDS_5 -> stringResource(R.string.cap_seconds_fmt, 5)
        MediaCollapseDelay.SECONDS_15 -> stringResource(R.string.cap_seconds_fmt, 15)
        MediaCollapseDelay.SECONDS_30 -> stringResource(R.string.cap_seconds_fmt, 30)
        MediaCollapseDelay.SECONDS_60 -> stringResource(R.string.cap_minutes_fmt, 1)
        MediaCollapseDelay.MINUTES_2 -> stringResource(R.string.cap_minutes_fmt, 2)
        MediaCollapseDelay.NEVER -> stringResource(R.string.cap_delay_always)
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
    Section(stringResource(R.string.cap_sec_controls)) {
        Text(
            stringResource(R.string.cap_controls_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(stringResource(R.string.cap_slider_style), style = MaterialTheme.typography.bodyMedium)
        Options(
            options = SliderStyle.entries,
            selected = settings.sliderStyle,
            label = {
                when (it) {
                    SliderStyle.PILL -> stringResource(R.string.cap_slider_pill)
                    SliderStyle.SPLIT -> stringResource(R.string.cap_slider_split)
                    SliderStyle.ROUNDED -> stringResource(R.string.cap_slider_rounded)
                }
            },
            onSelect = { style -> onUpdate { it.copy(sliderStyle = style) } }
        )
        PermissionRow(
            title = stringResource(R.string.cap_perm_brightness),
            granted = canWrite,
            onGrant = {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
        )
        PermissionRow(
            title = stringResource(R.string.cap_perm_silent),
            granted = canSilence,
            onGrant = { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        )
        Text(stringResource(R.string.cap_brightness_calibration), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.cap_brightness_calibration_desc, settings.brightnessMax),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val current = AndroidSettings.System.getInt(context.contentResolver, AndroidSettings.System.SCREEN_BRIGHTNESS, 0)
                if (current > 0) onUpdate { it.copy(brightnessMax = current) }
            }) { Text(stringResource(R.string.cap_calibrate)) }
            TextButton(onClick = { onUpdate { it.copy(brightnessMax = DEFAULT_BRIGHTNESS_MAX) } }) { Text(stringResource(R.string.cap_reset)) }
        }
    }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (granted) R.string.cap_allowed else R.string.cap_not_allowed),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        if (!granted) TextButton(onClick = onGrant) { Text(stringResource(R.string.cap_allow)) }
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

    Section(stringResource(R.string.cap_sec_events)) {
        SwitchRow(
            title = stringResource(R.string.cap_charging_card),
            description = stringResource(R.string.cap_charging_card_desc),
            checked = settings.chargingCardEnabled,
            onChange = { on -> onUpdate { it.copy(chargingCardEnabled = on) } }
        )
        SwitchRow(
            title = stringResource(R.string.cap_downloads_uploads),
            description = stringResource(R.string.cap_downloads_uploads_desc),
            checked = settings.progressEnabled,
            onChange = { on -> onUpdate { it.copy(progressEnabled = on) } }
        )
        SwitchRow(
            title = stringResource(R.string.cap_timers),
            description = stringResource(R.string.cap_timers_desc),
            checked = settings.timerEnabled,
            onChange = { on -> onUpdate { it.copy(timerEnabled = on) } }
        )
        Text(stringResource(R.string.cap_popups_for), style = MaterialTheme.typography.bodyLarge)
        EventCategory.entries.forEach { category ->
            SwitchRow(
                title = category.label,
                description = category.description,
                checked = category in settings.enabledEvents,
                onChange = { on -> toggle(category, on) }
            )
            if (category == EventCategory.HEADPHONES && category in settings.enabledEvents && !bluetoothAllowed) {
                TextButton(onClick = { requestBluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                    Text(stringResource(R.string.cap_allow_bluetooth))
                }
            }
        }
        Text(stringResource(R.string.cap_popup_duration), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniEventDuration.entries.forEach { duration ->
                FilterChip(
                    selected = settings.miniEventDuration == duration,
                    onClick = { onUpdate { it.copy(miniEventDuration = duration) } },
                    label = { Text(stringResource(R.string.cap_seconds_fmt, duration.millis / 1000)) }
                )
            }
        }
        IntSlider(stringResource(R.string.cap_low_battery_at), settings.lowBatteryThreshold, 5..30, unit = "%") { v ->
            onUpdate { it.copy(lowBatteryThreshold = v) }
        }
        SwitchRow(
            title = stringResource(R.string.cap_temp_f),
            description = null,
            checked = settings.temperatureFahrenheit,
            onChange = { on -> onUpdate { it.copy(temperatureFahrenheit = on) } }
        )
    }
}

private val EventCategory.label: String
    @Composable get() = stringResource(when (this) {
        EventCategory.CHARGING -> R.string.cap_event_charging
        EventCategory.LOW_BATTERY -> R.string.cap_event_low_battery
        EventCategory.RINGER -> R.string.cap_event_ringer
        EventCategory.HEADPHONES -> R.string.cap_event_headphones
        EventCategory.WIFI -> R.string.cap_event_wifi
        EventCategory.AIRPLANE -> R.string.cap_event_airplane
        EventCategory.UNLOCK -> R.string.cap_event_unlock
        EventCategory.BRIGHTNESS_MODE -> R.string.cap_event_brightness_mode
        EventCategory.DOWNLOADS -> R.string.cap_event_downloads
    })

private val EventCategory.description: String?
    @Composable get() = when (this) {
        EventCategory.HEADPHONES -> stringResource(R.string.cap_event_headphones_desc)
        EventCategory.LOW_BATTERY -> stringResource(R.string.cap_event_low_battery_desc)
        else -> null
    }

private val NotificationDelay.label: String
    @Composable get() = when (this) {
        NotificationDelay.SECONDS_1 -> stringResource(R.string.cap_seconds_fmt, 1)
        NotificationDelay.SECONDS_3 -> stringResource(R.string.cap_seconds_fmt, 3)
        NotificationDelay.SECONDS_5 -> stringResource(R.string.cap_seconds_fmt, 5)
        NotificationDelay.SECONDS_10 -> stringResource(R.string.cap_seconds_fmt, 10)
        NotificationDelay.SECONDS_20 -> stringResource(R.string.cap_seconds_fmt, 20)
        NotificationDelay.SECONDS_30 -> stringResource(R.string.cap_seconds_fmt, 30)
        NotificationDelay.UNTIL_OPENED -> stringResource(R.string.cap_delay_until_opened)
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
        Text(stringResource(R.string.cap_hidden_stay), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.cap_hidden_desc),
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
                    ) { Text(stringResource(R.string.cap_remove)) }
                }
            }
        TextButton(onClick = { pickingApp = true }) { Text(stringResource(R.string.cap_add_app)) }
    }

    if (pickingApp) {
        AppPickerDialog(
            title = stringResource(R.string.cap_pick_app_title),
            onPick = { app ->
                onUpdate { it.copy(hiddenPackages = it.hiddenPackages + app.packageName) }
                pickingApp = false
            },
            onDismiss = { pickingApp = false }
        )
    }
}

/** Dark enough to keep the white card text readable; the first entry (null) means "default". */
private val BackgroundSwatches = listOf(
    0xFF1A1615, 0xFF1B2A41, 0xFF1F3A2E, 0xFF3B1F2B, 0xFF2D2438, 0xFF3A2A1A
)
private val BorderSwatches = listOf(
    0xFFFFFFFF, 0xFFFFB690, 0xFF7FC8F8, 0xFF9BD67F, 0xFFF48FB1
)

/** A row of color dots plus a "default" chip; [selected] null = the default. */
@Composable
private fun SwatchRow(colors: List<Long>, selected: Long?, defaultLabel: String, onSelect: (Long?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(defaultLabel) })
        colors.forEachIndexed { index, argb ->
            val isSelected = selected == argb
            val swatchName = stringResource(R.string.a11y_swatch, index + 1)
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(argb) })
                    .semantics { contentDescription = swatchName }
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    // One element for TalkBack ("<title>, switch, on") and a tap target as wide as the row.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun IntSlider(label: String, value: Int, range: IntRange, unit: String = "dp", onChange: (Int) -> Unit) {
    Text("$label: $value $unit".replace(" %", "%"), style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        // The visible label is a separate Text, so name the slider and say its value with the unit.
        modifier = Modifier.semantics {
            contentDescription = label
            stateDescription = "$value $unit"
        }
    )
}

/** From this system font scale on, segmented rows give way to wrapping chips. */
private const val LARGE_FONT_SCALE = 1.3f

@Composable
private fun <T> Options(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    // Segments share one row, so at large font sizes their labels would be cut off: wrap chips instead.
    if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
        return
    }
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
