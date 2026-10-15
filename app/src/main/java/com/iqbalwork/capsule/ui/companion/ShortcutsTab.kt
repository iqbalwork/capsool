package com.iqbalwork.capsule.ui.companion

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.SystemShortcut
import java.util.UUID

/** Most tiles the Shortcuts card shows (two rows of four). */
const val MAX_SHORTCUTS = 8

private data class PickedContact(val name: String, val number: String)

/** F-54..F-56: build the Shortcuts card. Contacts come from the system picker, so no contacts permission. */
@Composable
fun ShortcutsTab(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pickingApp by remember { mutableStateOf(false) }
    var pickingSystem by remember { mutableStateOf(false) }
    var contact by remember { mutableStateOf<PickedContact?>(null) }
    val full = settings.shortcuts.size >= MAX_SHORTCUTS

    fun add(shortcut: Shortcut) = onUpdate { it.copy(shortcuts = (it.shortcuts + shortcut).take(MAX_SHORTCUTS)) }
    fun move(index: Int, step: Int) = onUpdate {
        val list = it.shortcuts.toMutableList()
        val target = index + step
        if (target in list.indices) list.add(target, list.removeAt(index))
        it.copy(shortcuts = list)
    }

    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        // The picker grants one-time read access to the chosen row only.
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) contact = PickedContact(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty())
        }
    }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shortcuts card", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Long-press the capsule while nothing is showing to open it (change this in Gestures). " +
                            "Up to $MAX_SHORTCUTS shortcuts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { pickingApp = true }, enabled = !full) { Text("Add app") }
                        FilledTonalButton(
                            onClick = { pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
                            enabled = !full
                        ) { Text("Add contact") }
                        FilledTonalButton(onClick = { pickingSystem = true }, enabled = !full) { Text("Add system") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Themed app icons", style = MaterialTheme.typography.bodyLarge)
                            Text("Uses each app's monochrome icon in your wallpaper colors (Android 13+).",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.themedShortcutIcons, onCheckedChange = { on -> onUpdate { it.copy(themedShortcutIcons = on) } })
                    }
                }
            }
        }
        itemsIndexed(settings.shortcuts, key = { _, shortcut -> shortcut.id }) { index, shortcut ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(shortcut.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (shortcut.kind) {
                            ShortcutKind.APP -> "App"
                            ShortcutKind.CONTACT -> if (shortcut.contactAction == ContactAction.CALL) "Call ${shortcut.target}" else "WhatsApp ${shortcut.target}"
                            ShortcutKind.SYSTEM -> "System"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { move(index, -1) }, enabled = index > 0) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { move(index, 1) }, enabled = index < settings.shortcuts.lastIndex) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Move down")
                }
                IconButton(onClick = { onUpdate { s -> s.copy(shortcuts = s.shortcuts.filter { it.id != shortcut.id }) } }) {
                    Icon(Icons.Rounded.Delete, "Remove")
                }
            }
        }
    }

    if (pickingApp) {
        AppPickerDialog(
            title = "Add app",
            onPick = { app ->
                add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.APP, app.label, app.packageName))
                pickingApp = false
            },
            onDismiss = { pickingApp = false }
        )
    }
    if (pickingSystem) {
        AlertDialog(
            onDismissRequest = { pickingSystem = false },
            title = { Text("Add system shortcut") },
            text = {
                Column {
                    SystemShortcut.entries.forEach { screen ->
                        TextButton(onClick = {
                            add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.SYSTEM, screen.label, screen.name))
                            pickingSystem = false
                        }) { Text(screen.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingSystem = false }) { Text("Cancel") } }
        )
    }
    contact?.let { picked ->
        AlertDialog(
            onDismissRequest = { contact = null },
            title = { Text(picked.name.ifBlank { picked.number }) },
            text = { Text("What should tapping this contact do?") },
            confirmButton = {
                TextButton(onClick = {
                    add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.CONTACT, picked.name.ifBlank { picked.number }, picked.number, ContactAction.CALL))
                    contact = null
                }) { Text("Call") }
            },
            dismissButton = {
                TextButton(onClick = {
                    add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.CONTACT, picked.name.ifBlank { picked.number }, picked.number, ContactAction.WHATSAPP))
                    contact = null
                }) { Text("WhatsApp") }
            }
        )
    }
}
