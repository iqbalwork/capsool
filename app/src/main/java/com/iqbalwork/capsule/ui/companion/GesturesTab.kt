package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.platform.Automation
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.iqbalwork.capsule.R

/** F-70..F-77: per-gesture actions in two contexts, plus the automation broadcast. */
@Composable
fun GesturesTab(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GestureSection(
                title = stringResource(R.string.gx_gest_active_title),
                description = stringResource(R.string.gx_gest_active_desc),
                active = true,
                settings = settings,
                onUpdate = onUpdate
            )
        }
        item {
            GestureSection(
                title = stringResource(R.string.gx_gest_idle_title),
                description = stringResource(R.string.gx_gest_idle_desc),
                active = false,
                settings = settings,
                onUpdate = onUpdate
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.gx_automation), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().toggleable(value = settings.automationBroadcast, role = Role.Switch, onValueChange = { on -> onUpdate { it.copy(automationBroadcast = on) } })) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.gx_broadcast_gestures), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.gx_broadcast_desc, Automation.ACTION_EVENT),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.automationBroadcast, onCheckedChange = null)
                    }
                    TextButton(onClick = { Automation.sendGesture(context, gesture = "test", action = "none", card = null) }) {
                        Text(stringResource(R.string.gx_send_test_broadcast))
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureSection(
    title: String,
    description: String,
    active: Boolean,
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit
) {
    fun set(type: GestureType, binding: GestureBinding) = onUpdate {
        if (active) it.copy(gesturesActive = it.gesturesActive + (type to binding))
        else it.copy(gesturesIdle = it.gesturesIdle + (type to binding))
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GestureType.entries.forEach { type ->
                GestureRow(type, settings.gesture(type, active), active, settings.customIntents) { set(type, it) }
            }
        }
    }
}

@Composable
private fun GestureRow(
    type: GestureType,
    binding: GestureBinding,
    active: Boolean,
    customIntents: List<CustomIntent>,
    onChange: (GestureBinding) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var pickingApp by remember { mutableStateOf(false) }
    var pickingIntent by remember { mutableStateOf(false) }
    // Swipes switch cards only when there are cards; on the idle pill they are free for other actions.
    val actions = GestureAction.entries.filter { active || (it != GestureAction.NEXT_CARD && it != GestureAction.PREVIOUS_CARD) }
    val intentName = if (binding.action == GestureAction.CUSTOM_INTENT) {
        customIntents.firstOrNull { it.id == binding.target }?.label?.ifEmpty { null }
            ?: binding.target
            ?: stringResource(R.string.gx_custom_action)
    } else null
    val buttonLabel = when (binding.action) {
        GestureAction.OPEN_APP -> stringResource(R.string.gx_open_app_target, binding.target ?: stringResource(R.string.gx_app))
        GestureAction.CUSTOM_INTENT -> intentName ?: stringResource(R.string.gx_a_custom_intent)
        else -> stringResource(binding.action.labelRes)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(type.labelRes), style = MaterialTheme.typography.bodyLarge)
            if (type == GestureType.DOUBLE_TAP && binding.action != GestureAction.NONE) {
                Text(stringResource(R.string.gx_double_tap_note), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column {
            TextButton(onClick = { menuOpen = true }) {
                Text(buttonLabel)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(text = { Text(stringResource(action.labelRes)) }, onClick = {
                        menuOpen = false
                        when (action) {
                            GestureAction.OPEN_APP -> pickingApp = true
                            GestureAction.CUSTOM_INTENT -> pickingIntent = true
                            else -> onChange(GestureBinding(action))
                        }
                    })
                }
            }
        }
    }
    if (pickingApp) {
        AppPickerDialog(
            title = stringResource(R.string.gx_open_which_app),
            onPick = { app ->
                onChange(GestureBinding(GestureAction.OPEN_APP, app.packageName))
                pickingApp = false
            },
            onDismiss = { pickingApp = false }
        )
    }
    if (pickingIntent) {
        CustomIntentPickerDialog(
            intents = customIntents,
            onPick = { intent ->
                onChange(GestureBinding(GestureAction.CUSTOM_INTENT, intent.id))
                pickingIntent = false
            },
            onDismiss = { pickingIntent = false }
        )
    }
}

@Composable
private fun CustomIntentPickerDialog(
    intents: List<CustomIntent>,
    onPick: (CustomIntent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gx_choose_custom_action)) },
        text = {
            if (intents.isEmpty()) {
                Text(
                    stringResource(R.string.gx_no_custom_actions),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(intents, key = { it.id }) { intent ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(intent) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    intent.label.ifEmpty { intent.action },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "${intent.type.name} • ${intent.action}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.gx_close)) }
        }
    )
}

private val GestureType.labelRes: Int
    @StringRes get() = when (this) {
        GestureType.TAP -> R.string.gx_g_tap
        GestureType.DOUBLE_TAP -> R.string.gx_g_double_tap
        GestureType.LONG_PRESS -> R.string.gx_g_long_press
        GestureType.SWIPE_LEFT -> R.string.gx_g_swipe_left
        GestureType.SWIPE_RIGHT -> R.string.gx_g_swipe_right
    }

val GestureAction.labelRes: Int
    @StringRes get() = when (this) {
        GestureAction.NONE -> R.string.gx_a_none
        GestureAction.TOGGLE_EXPAND -> R.string.gx_a_toggle_expand
        GestureAction.NEXT_CARD -> R.string.gx_a_next_card
        GestureAction.PREVIOUS_CARD -> R.string.gx_a_previous_card
        GestureAction.MEDIA_PLAY_PAUSE -> R.string.gx_a_media_play_pause
        GestureAction.MEDIA_NEXT -> R.string.gx_a_media_next
        GestureAction.MEDIA_PREVIOUS -> R.string.gx_a_media_previous
        GestureAction.OPEN_CONTROLS -> R.string.gx_a_open_controls
        GestureAction.OPEN_SHORTCUTS -> R.string.gx_a_open_shortcuts
        GestureAction.TOGGLE_FLASHLIGHT -> R.string.gx_a_toggle_flashlight
        GestureAction.OPEN_NOTIFICATIONS -> R.string.gx_a_open_notifications
        GestureAction.OPEN_QUICK_SETTINGS -> R.string.gx_a_open_quick_settings
        GestureAction.SCREENSHOT -> R.string.gx_a_screenshot
        GestureAction.LOCK_SCREEN -> R.string.gx_a_lock_screen
        GestureAction.SCROLL_TO_TOP -> R.string.gx_a_scroll_to_top
        GestureAction.OPEN_APP -> R.string.gx_a_open_app
        GestureAction.CUSTOM_INTENT -> R.string.gx_a_custom_intent
    }
