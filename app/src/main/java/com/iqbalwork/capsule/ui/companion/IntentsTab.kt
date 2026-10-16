package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType
import com.iqbalwork.capsule.core.settings.CustomIntentValidator
import com.iqbalwork.capsule.core.settings.CustomIntentValidator.isValid
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.platform.intents.CustomIntentRunner
import com.iqbalwork.capsule.platform.intents.IntentResult
import com.iqbalwork.capsule.ui.components.AvailableIntentIcons
import com.iqbalwork.capsule.ui.components.customIntentIcon
import java.util.UUID
import androidx.compose.ui.res.stringResource
import com.iqbalwork.capsule.R

/**
 * F-76: Custom intent builder UI for Activity / Service / Broadcast actions.
 * Allows adding, editing, deleting, and testing custom intents.
 */
@Composable
fun IntentsTab(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val runner = remember(context) { CustomIntentRunner(context) }
    var editingIntent by remember { mutableStateOf<CustomIntent?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<Map<String, IntentResult>>(emptyMap()) }

    fun save(intent: CustomIntent) = onUpdate { current ->
        val existingIndex = current.customIntents.indexOfFirst { it.id == intent.id }
        val updatedList = if (existingIndex >= 0) {
            current.customIntents.toMutableList().apply { set(existingIndex, intent) }
        } else {
            current.customIntents + intent
        }
        current.copy(customIntents = updatedList)
    }

    fun delete(id: String) = onUpdate { current ->
        current.copy(customIntents = current.customIntents.filterNot { it.id == id })
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.gx_intent_builder), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.gx_intent_builder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = { isCreating = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.gx_add_custom_action))
                    }
                }
            }
        }

        if (settings.customIntents.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.gx_no_intents_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(settings.customIntents, key = { it.id }) { intent ->
                val result = testResults[intent.id]
                IntentItemCard(
                    intent = intent,
                    testResult = result,
                    onTest = {
                        val res = runner.run(intent)
                        testResults = testResults + (intent.id to res)
                    },
                    onEdit = { editingIntent = intent },
                    onDelete = { delete(intent.id) }
                )
            }
        }
    }

    if (isCreating) {
        IntentEditDialog(
            initial = null,
            onSave = {
                save(it)
                isCreating = false
            },
            onDismiss = { isCreating = false }
        )
    }

    editingIntent?.let { intent ->
        IntentEditDialog(
            initial = intent,
            onSave = {
                save(it)
                editingIntent = null
            },
            onDismiss = { editingIntent = null }
        )
    }
}

@Composable
private fun IntentItemCard(
    intent: CustomIntent,
    testResult: IntentResult?,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = customIntentIcon(intent.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        intent.label.ifEmpty { intent.action },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${intent.type.name} • ${intent.action}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.gx_edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.gx_delete))
                }
            }

            if (!intent.component.isNullOrBlank()) {
                Text(
                    stringResource(R.string.gx_component_line, intent.component),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (intent.extras.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.gx_extras_line,
                        intent.extras.size,
                        intent.extras.entries.joinToString(", ") { "${it.key}=${it.value}" }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onTest) {
                    Text(stringResource(R.string.gx_send_test))
                }
                Spacer(Modifier.width(12.dp))
                if (testResult != null) {
                    when (testResult) {
                        is IntentResult.Ok -> Text(
                            stringResource(R.string.gx_test_ok),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        is IntentResult.Error -> Text(
                            stringResource(R.string.gx_test_error, testResult.message.asString()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentEditDialog(
    initial: CustomIntent?,
    onSave: (CustomIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: CustomIntentType.BROADCAST) }
    var action by remember { mutableStateOf(initial?.action.orEmpty()) }
    var component by remember { mutableStateOf(initial?.component.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: "bolt") }
    var extras by remember { mutableStateOf(initial?.extras?.toList() ?: emptyList()) }

    var typeMenuOpen by remember { mutableStateOf(false) }
    var iconMenuOpen by remember { mutableStateOf(false) }

    val validation = remember(action, component) {
        CustomIntentValidator.validate(action, component.ifBlank { null })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.gx_new_custom_action else R.string.gx_edit_custom_action)) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.gx_label)) },
                        placeholder = { Text(stringResource(R.string.gx_label_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.gx_type), style = MaterialTheme.typography.labelMedium)
                            TextButton(
                                onClick = { typeMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(type.name)
                            }
                            DropdownMenu(
                                expanded = typeMenuOpen,
                                onDismissRequest = { typeMenuOpen = false }
                            ) {
                                CustomIntentType.entries.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name) },
                                        onClick = {
                                            type = t
                                            typeMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.gx_icon), style = MaterialTheme.typography.labelMedium)
                            TextButton(
                                onClick = { iconMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = customIntentIcon(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(icon)
                            }
                            DropdownMenu(
                                expanded = iconMenuOpen,
                                onDismissRequest = { iconMenuOpen = false }
                            ) {
                                AvailableIntentIcons.forEach { name ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = customIntentIcon(name),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = { Text(name) },
                                        onClick = {
                                            icon = name
                                            iconMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = action,
                        onValueChange = { action = it },
                        label = { Text(stringResource(R.string.gx_action_field)) },
                        placeholder = { Text(stringResource(R.string.gx_action_placeholder)) },
                        singleLine = true,
                        isError = action.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = component,
                        onValueChange = { component = it },
                        label = { Text(stringResource(R.string.gx_component_field)) },
                        placeholder = { Text(stringResource(R.string.gx_component_placeholder)) },
                        singleLine = true,
                        isError = validation is CustomIntentValidator.Result.Error && component.isNotBlank(),
                        supportingText = {
                            if (validation is CustomIntentValidator.Result.Error && component.isNotBlank()) {
                                Text(validation.message.asString(), color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(stringResource(R.string.gx_component_hint))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.gx_extras_title), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = { extras = extras + ("" to "") }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.gx_add_extra))
                        }
                    }
                }

                items(extras.indices.toList()) { index ->
                    val pair = extras[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pair.first,
                            onValueChange = { newKey ->
                                val list = extras.toMutableList()
                                list[index] = newKey to pair.second
                                extras = list
                            },
                            label = { Text(stringResource(R.string.gx_key)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { newVal ->
                                val list = extras.toMutableList()
                                list[index] = pair.first to newVal
                                extras = list
                            },
                            label = { Text(stringResource(R.string.gx_value)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val list = extras.toMutableList()
                            list.removeAt(index)
                            extras = list
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.gx_remove_extra))
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val extrasMap = extras
                        .filter { it.first.isNotBlank() }
                        .toMap()
                    val result = CustomIntent(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        label = label.trim(),
                        type = type,
                        action = action.trim(),
                        component = component.trim().ifBlank { null },
                        extras = extrasMap,
                        icon = icon.trim().ifBlank { "bolt" }
                    )
                    onSave(result)
                },
                enabled = validation.isValid
            ) {
                Text(stringResource(R.string.gx_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gx_cancel))
            }
        }
    )
}
