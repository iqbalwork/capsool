package com.iqbalwork.capsule.ui.components

import com.iqbalwork.capsule.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.SystemShortcut
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.platform.barcode.BarcodeValidator

private const val COLUMNS = 4
private const val MAX_TILES = 8

/** F-54..F-56: up to eight app, contact and system shortcuts in two rows. */
@Composable
fun ExpandedShortcutsCard(
    shortcuts: List<Shortcut>,
    themedIcons: Boolean,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier,
    barcode: BarcodeContent = BarcodeContent(),
    barcodeEnabled: Boolean = true
) {
    val showBarcode = barcodeEnabled && barcode.enabled && remember(barcode.content, barcode.format) {
        BarcodeValidator.validate(barcode.content, barcode.format).isValid
    }

    if (!showBarcode && shortcuts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.st_shortcuts_empty),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (showBarcode && shortcuts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpandedBarcodeCard(
                barcode = barcode,
                plateHeight = 100.dp,
                showLabel = true
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showBarcode) {
            ExpandedBarcodeCard(
                barcode = barcode,
                plateHeight = 56.dp,
                showLabel = true
            )
            Spacer(Modifier.height(4.dp))
        }
        val maxTiles = if (showBarcode) 4 else MAX_TILES
        shortcuts.take(maxTiles).chunked(COLUMNS).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        themedIcons = themedIcons,
                        onClick = { onAction(UserIntent.LaunchShortcut(shortcut.id)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ShortcutTile(shortcut: Shortcut, themedIcons: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            when (shortcut.kind) {
                ShortcutKind.APP -> AppTileIcon(shortcut.target, themedIcons)
                ShortcutKind.CONTACT -> ContactTileIcon(shortcut)
                ShortcutKind.SYSTEM -> Icon(
                    systemIcon(shortcut.target),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            shortcut.label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppTileIcon(packageName: String, themed: Boolean) {
    val icon by rememberAppIcon(packageName, themed)
    val current = icon ?: return
    val bitmap = remember(current) { current.bitmap.asImageBitmap() }
    if (current.monochrome) {
        Image(bitmap, contentDescription = null, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary), modifier = Modifier.size(40.dp))
    } else {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ContactTileIcon(shortcut: Shortcut) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            shortcut.label.take(1).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge
        )
        Icon(
            if (shortcut.contactAction == ContactAction.CALL) Icons.Rounded.Call else Icons.AutoMirrored.Rounded.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

fun systemIcon(target: String): ImageVector = when (runCatching { SystemShortcut.valueOf(target) }.getOrNull()) {
    SystemShortcut.INTERNET_PANEL -> Icons.Rounded.NetworkCheck
    SystemShortcut.WIFI_PANEL -> Icons.Rounded.Wifi
    SystemShortcut.VOLUME_PANEL -> Icons.AutoMirrored.Rounded.VolumeUp
    SystemShortcut.BLUETOOTH -> Icons.Rounded.Bluetooth
    SystemShortcut.DISPLAY -> Icons.Rounded.SettingsBrightness
    SystemShortcut.LOCATION -> Icons.Rounded.LocationOn
    SystemShortcut.BATTERY, null -> Icons.Rounded.BatteryStd
}
