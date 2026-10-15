package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.platform.Automation

/** F-70..F-77: per-gesture actions in two contexts, plus the automation broadcast. */
@Composable
fun GesturesTab(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GestureSection(
                title = "While something is showing",
                description = "Music, a notification, a timer, charging… Swipes move between cards.",
                active = true,
                settings = settings,
                onUpdate = onUpdate
            )
        }
        item {
            GestureSection(
                title = "When the capsule is idle",
                description = "Nothing going on: the plain pill over the camera.",
                active = false,
                settings = settings,
                onUpdate = onUpdate
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automation", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Broadcast gestures", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Tasker, MacroDroid or Automate can react with an \"Intent received\" trigger for " +
                                    "${Automation.ACTION_EVENT}. Extras: type, gesture, action, card.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.automationBroadcast, onCheckedChange = { on -> onUpdate { it.copy(automationBroadcast = on) } })
                    }
                    TextButton(onClick = { Automation.sendGesture(context, gesture = "test", action = "none", card = null) }) {
                        Text("Send test broadcast")
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
                GestureRow(type, settings.gesture(type, active), active) { set(type, it) }
            }
        }
    }
}

@Composable
private fun GestureRow(type: GestureType, binding: GestureBinding, active: Boolean, onChange: (GestureBinding) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var pickingApp by remember { mutableStateOf(false) }
    // Swipes switch cards only when there are cards; on the idle pill they are free for other actions.
    val actions = GestureAction.entries.filter { active || (it != GestureAction.NEXT_CARD && it != GestureAction.PREVIOUS_CARD) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(type.label, style = MaterialTheme.typography.bodyLarge)
            if (type == GestureType.DOUBLE_TAP && binding.action != GestureAction.NONE) {
                Text("Single taps wait a moment to tell them apart.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column {
            TextButton(onClick = { menuOpen = true }) {
                Text(if (binding.action == GestureAction.OPEN_APP) "Open ${binding.target ?: "app"}" else binding.action.label)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                actions.forEach { action ->
                    DropdownMenuItem(text = { Text(action.label) }, onClick = {
                        menuOpen = false
                        if (action == GestureAction.OPEN_APP) pickingApp = true else onChange(GestureBinding(action))
                    })
                }
            }
        }
    }
    if (pickingApp) {
        AppPickerDialog(
            title = "Open which app?",
            onPick = { app ->
                onChange(GestureBinding(GestureAction.OPEN_APP, app.packageName))
                pickingApp = false
            },
            onDismiss = { pickingApp = false }
        )
    }
}

private val GestureType.label: String
    get() = when (this) {
        GestureType.TAP -> "Tap"
        GestureType.DOUBLE_TAP -> "Double tap"
        GestureType.LONG_PRESS -> "Long press"
        GestureType.SWIPE_LEFT -> "Swipe left"
        GestureType.SWIPE_RIGHT -> "Swipe right"
    }

val GestureAction.label: String
    get() = when (this) {
        GestureAction.NONE -> "Nothing"
        GestureAction.TOGGLE_EXPAND -> "Open / close card"
        GestureAction.NEXT_CARD -> "Next card"
        GestureAction.PREVIOUS_CARD -> "Previous card"
        GestureAction.MEDIA_PLAY_PAUSE -> "Play / pause"
        GestureAction.MEDIA_NEXT -> "Next track"
        GestureAction.MEDIA_PREVIOUS -> "Previous track"
        GestureAction.OPEN_CONTROLS -> "Controls"
        GestureAction.OPEN_SHORTCUTS -> "Shortcuts"
        GestureAction.TOGGLE_FLASHLIGHT -> "Flashlight"
        GestureAction.OPEN_NOTIFICATIONS -> "Notification shade"
        GestureAction.OPEN_QUICK_SETTINGS -> "Quick settings"
        GestureAction.SCREENSHOT -> "Screenshot"
        GestureAction.LOCK_SCREEN -> "Lock screen"
        GestureAction.SCROLL_TO_TOP -> "Scroll to top"
        GestureAction.OPEN_APP -> "Open app…"
    }
