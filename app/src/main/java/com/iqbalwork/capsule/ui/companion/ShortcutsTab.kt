package com.iqbalwork.capsule.ui.companion

import android.app.Activity
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.iqbalwork.capsule.model.BarcodeFormat
import com.iqbalwork.capsule.platform.barcode.BarcodeEncoder
import com.iqbalwork.capsule.platform.barcode.BarcodeValidationResult
import com.iqbalwork.capsule.platform.barcode.BarcodeValidator
import com.iqbalwork.capsule.ui.components.ExpandedBarcodeCard
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
import androidx.compose.ui.res.stringResource
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.platform.barcode.toUiText
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.SystemShortcut
import com.iqbalwork.capsule.platform.barcode.BarcodeScanResult
import com.iqbalwork.capsule.platform.barcode.scanImage
import com.iqbalwork.capsule.platform.barcode.scanWithPlayServices
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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

    val scope = rememberCoroutineScope()
    var scanningImage by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<UiText?>(null) }

    // Both entry points (gallery image, camera) end in the same three outcomes.
    val applyScanResult: (BarcodeScanResult) -> Unit = { result ->
        when (result) {
            is BarcodeScanResult.Found -> {
                scanError = null
                onUpdate { s ->
                    s.copy(barcode = s.barcode.copy(
                        content = result.code.content,
                        format = result.code.format,
                    ))
                }
            }
            is BarcodeScanResult.Failed -> scanError = result.message
            // The user backed out of the camera scanner; that is not an error.
            BarcodeScanResult.Cancelled -> scanError = null
        }
    }

    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanError = null
        scanningImage = true
        scope.launch {
            applyScanResult(scanImage(context, uri, settings.barcode.format))
            scanningImage = false
        }
    }

    val startCameraScan: () -> Unit = {
        scanError = null
        scanningImage = true
        scope.launch {
            applyScanResult(scanWithPlayServices(context, settings.barcode.format))
            scanningImage = false
        }
    }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sc_shortcuts_card_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.sc_shortcuts_card_desc, MAX_SHORTCUTS),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { pickingApp = true }, enabled = !full) { Text(stringResource(R.string.sc_add_app)) }
                        FilledTonalButton(
                            onClick = { pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) },
                            enabled = !full
                        ) { Text(stringResource(R.string.sc_add_contact)) }
                        FilledTonalButton(onClick = { pickingSystem = true }, enabled = !full) { Text(stringResource(R.string.sc_add_system)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().toggleable(value = settings.themedShortcutIcons, role = Role.Switch, onValueChange = { on -> onUpdate { it.copy(themedShortcutIcons = on) } })) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sc_themed_icons_title), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.sc_themed_icons_desc),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.themedShortcutIcons, onCheckedChange = null)
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().toggleable(value = settings.barcode.enabled, role = Role.Switch, onValueChange = { on -> onUpdate { it.copy(barcode = it.barcode.copy(enabled = on)) } })) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sc_barcode_card_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.sc_barcode_card_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.barcode.enabled, onCheckedChange = null)
                    }

                    var formatMenuExpanded by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sc_format), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Box {
                            OutlinedButton(onClick = { formatMenuExpanded = true }) {
                                Text(settings.barcode.format.displayName)
                            }
                            DropdownMenu(
                                expanded = formatMenuExpanded,
                                onDismissRequest = { formatMenuExpanded = false }
                            ) {
                                BarcodeFormat.entries.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format.displayName) },
                                        onClick = {
                                            onUpdate { it.copy(barcode = it.barcode.copy(format = format)) }
                                            formatMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val validation = remember(settings.barcode.content, settings.barcode.format) {
                        BarcodeValidator.validate(settings.barcode.content, settings.barcode.format)
                    }
                    val encodeResult = remember(settings.barcode.content, settings.barcode.format) {
                        if (validation is BarcodeValidationResult.Ok && settings.barcode.content.isNotEmpty()) {
                            runCatching { BarcodeEncoder.encode(settings.barcode.content, settings.barcode.format) }
                        } else {
                            null
                        }
                    }
                    val encoderError = encodeResult?.exceptionOrNull()?.toUiText()
                    val encoderErrorText = encoderError?.asString()
                    val barcodeErrorMessage = when {
                        validation is BarcodeValidationResult.Error -> validation.message.asString()
                        encoderErrorText != null -> stringResource(R.string.sc_barcode_build_error, encoderErrorText)
                        else -> null
                    }

                    OutlinedTextField(
                        value = settings.barcode.content,
                        onValueChange = { newContent ->
                            onUpdate { it.copy(barcode = it.barcode.copy(content = newContent)) }
                        },
                        label = { Text(stringResource(R.string.sc_content_code)) },
                        placeholder = {
                            Text(when (settings.barcode.format) {
                                BarcodeFormat.QR_CODE -> "https://example.com"
                                BarcodeFormat.EAN_13 -> "4006381333931"
                                BarcodeFormat.EAN_8 -> "96385074"
                                BarcodeFormat.UPC_A -> "036000291452"
                                BarcodeFormat.UPC_E -> "01234565"
                                BarcodeFormat.ITF -> "123456"
                                BarcodeFormat.CODE_39 -> "CODE39"
                                BarcodeFormat.CODE_93 -> "CODE93"
                                BarcodeFormat.CODE_128 -> "Code 128"
                                BarcodeFormat.CODABAR -> "A123456B"
                            })
                        },
                        isError = barcodeErrorMessage != null && settings.barcode.content.isNotEmpty(),
                        supportingText = {
                            if (barcodeErrorMessage != null && settings.barcode.content.isNotEmpty()) {
                                Text(
                                    text = barcodeErrorMessage,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = settings.barcode.format != BarcodeFormat.QR_CODE,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            enabled = !scanningImage
                        ) {
                            Text(stringResource(R.string.sc_scan_image))
                        }
                        // Runs inside Play Services, so it needs no camera permission.
                        OutlinedButton(
                            onClick = startCameraScan,
                            enabled = !scanningImage
                        ) {
                            Text(stringResource(R.string.sc_scan_camera))
                        }
                    }
                    if (scanningImage) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    scanError?.let { message ->
                        Text(
                            text = message.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    OutlinedTextField(
                        value = settings.barcode.label,
                        onValueChange = { newLabel ->
                            onUpdate { it.copy(barcode = it.barcode.copy(label = newLabel)) }
                        },
                        label = { Text(stringResource(R.string.sc_label_optional)) },
                        placeholder = { Text(stringResource(R.string.sc_label_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.sc_barcode_color), style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val presetColors = listOf(
                                0xFF000000L to "Black",
                                0xFF0D47A1L to "Navy",
                                0xFF1B5E20L to "Green",
                                0xFFB71C1CL to "Red",
                                0xFF4A148CL to "Purple",
                                0xFFE65100L to "Orange"
                            )
                            presetColors.forEach { (colorLong, _) ->
                                val isSelected = settings.barcode.color == colorLong
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorLong))
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                        )
                                        .clickable(role = Role.Button) {
                                            onUpdate { it.copy(barcode = it.barcode.copy(color = colorLong)) }
                                        }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().toggleable(value = settings.barcode.flipped, role = Role.Switch, onValueChange = { on -> onUpdate { it.copy(barcode = it.barcode.copy(flipped = on)) } })) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.sc_flip_title), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.sc_flip_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.barcode.flipped, onCheckedChange = null)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.sc_live_preview), style = MaterialTheme.typography.titleSmall)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1B1A))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (validation is BarcodeValidationResult.Ok && encoderError == null) {
                                ExpandedBarcodeCard(
                                    barcode = settings.barcode,
                                    plateHeight = 90.dp,
                                    showLabel = true
                                )
                            } else {
                                val previewReason = barcodeErrorMessage ?: stringResource(R.string.sc_preview_invalid)
                                Text(
                                    text = stringResource(R.string.sc_preview_unavailable, previewReason),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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
                            ShortcutKind.APP -> stringResource(R.string.sc_kind_app)
                            ShortcutKind.CONTACT -> if (shortcut.contactAction == ContactAction.CALL) {
                                stringResource(R.string.sc_kind_call, shortcut.target)
                            } else {
                                stringResource(R.string.sc_kind_whatsapp, shortcut.target)
                            }
                            ShortcutKind.SYSTEM -> stringResource(R.string.sc_kind_system)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { move(index, -1) }, enabled = index > 0) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.sc_move_up)) }
                IconButton(onClick = { move(index, 1) }, enabled = index < settings.shortcuts.lastIndex) {
                    Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.sc_move_down))
                }
                IconButton(onClick = { onUpdate { s -> s.copy(shortcuts = s.shortcuts.filter { it.id != shortcut.id }) } }) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.sc_remove))
                }
            }
        }
    }

    if (pickingApp) {
        AppPickerDialog(
            title = stringResource(R.string.sc_add_app),
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
            title = { Text(stringResource(R.string.sc_add_system_title)) },
            text = {
                Column {
                    SystemShortcut.entries.forEach { screen ->
                        val screenName = stringResource(screen.labelRes)
                        TextButton(onClick = {
                            add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.SYSTEM, screenName, screen.name))
                            pickingSystem = false
                        }) { Text(screenName) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingSystem = false }) { Text(stringResource(R.string.sc_cancel)) } }
        )
    }
    contact?.let { picked ->
        AlertDialog(
            onDismissRequest = { contact = null },
            title = { Text(picked.name.ifBlank { picked.number }) },
            text = { Text(stringResource(R.string.sc_contact_prompt)) },
            confirmButton = {
                TextButton(onClick = {
                    add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.CONTACT, picked.name.ifBlank { picked.number }, picked.number, ContactAction.CALL))
                    contact = null
                }) { Text(stringResource(R.string.sc_call)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    add(Shortcut(UUID.randomUUID().toString(), ShortcutKind.CONTACT, picked.name.ifBlank { picked.number }, picked.number, ContactAction.WHATSAPP))
                    contact = null
                }) { Text(stringResource(R.string.sc_whatsapp)) }
            }
        )
    }
}
