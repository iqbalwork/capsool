package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.StaticCard
import com.iqbalwork.capsule.core.settings.StaticCardType
import com.iqbalwork.capsule.core.settings.StaticCardViewMode
import com.iqbalwork.capsule.core.settings.moveDown
import com.iqbalwork.capsule.core.settings.moveUp
import com.iqbalwork.capsule.core.settings.seedStaticCards
import com.iqbalwork.capsule.core.settings.without
import java.util.UUID

/**
 * Manages the static cards: view mode, order, which one is the start card, and which types
 * exist. Drawing the cards is the overlay's job, not this tab's.
 */
@Composable
fun StaticCardsTab(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDerived = settings.staticCards.isEmpty()
    val displayCards = settings.effectiveStaticCards
    val existingTypes = displayCards.map { it.type }.toSet()
    val availableTypes = StaticCardType.entries.filter { it !in existingTypes }

    var addMenuExpanded by remember { mutableStateOf(false) }
    var dialogType by remember { mutableStateOf<StaticCardType?>(null) }
    var dialogLabel by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode tampilan: Carousel vs List
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("View mode", style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        StaticCardViewMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.staticCardViewMode == mode,
                                onClick = { onUpdate { it.copy(staticCardViewMode = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(index, StaticCardViewMode.entries.size)
                            ) {
                                Text(if (mode == StaticCardViewMode.CAROUSEL) "Carousel" else "List")
                            }
                        }
                    }
                    Text(
                        if (settings.staticCardViewMode == StaticCardViewMode.CAROUSEL) {
                            "Carousel: swipe left and right to move between cards."
                        } else {
                            "List view is not drawn on the capsule yet — cards still appear as a carousel."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Derived mode: explain where the cards come from, offer to make them explicit
        if (isDerived) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cards are automatic", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "These cards come from the shortcut, barcode and custom-action settings. " +
                                "Edit them to set the order, pick a start card, or add cards by hand.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = { onUpdate { it.seedStaticCards() } },
                            enabled = displayCards.isNotEmpty()
                        ) {
                            Text("Edit cards")
                        }
                    }
                }
            }
        }

        // Card list header plus the "Add card" menu
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cards", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The order these cards appear in on the capsule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            FilledTonalButton(
                                onClick = { addMenuExpanded = true },
                                enabled = availableTypes.isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add card")
                            }
                            DropdownMenu(
                                expanded = addMenuExpanded,
                                onDismissRequest = { addMenuExpanded = false }
                            ) {
                                availableTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            addMenuExpanded = false
                                            dialogType = type
                                            dialogLabel = type.label
                                            showAddDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Kartu kosong
        if (displayCards.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No static cards yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(displayCards, key = { _, card -> card.id }) { index, card ->
                val isStart = settings.effectiveStartCardId == card.id
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(card.label, style = MaterialTheme.typography.titleSmall)
                                if (isStart) {
                                    Text(
                                        "• Start",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            // The type only adds information once the card has been renamed.
                            if (card.label != card.type.label) {
                                Text(
                                    card.type.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isDerived) {
                            Text(
                                "Automatic",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    onUpdate { s ->
                                        val nextStartId = if (s.effectiveStartCardId == card.id) null else card.id
                                        s.copy(startCardId = nextStartId)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isStart) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = if (isStart) "Unset start card" else "Set as start card",
                                    tint = if (isStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onUpdate { s -> s.copy(staticCards = s.staticCards.moveUp(card.id)) } },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = { onUpdate { s -> s.copy(staticCards = s.staticCards.moveDown(card.id)) } },
                                enabled = index < displayCards.lastIndex
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
                            }
                            IconButton(
                                onClick = {
                                    onUpdate { s ->
                                        val nextStartId = if (s.startCardId == card.id) null else s.startCardId
                                        s.copy(
                                            staticCards = s.staticCards.without(card.id),
                                            startCardId = nextStartId
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val type = dialogType
        if (type != null) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add ${type.label} card") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialogLabel,
                            onValueChange = { dialogLabel = it },
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = {
                            val label = dialogLabel.trim().ifEmpty { type.label }
                            val newCard = StaticCard(
                                id = UUID.randomUUID().toString(),
                                type = type,
                                label = label
                            )
                            onUpdate { current ->
                                val base = current.seedStaticCards()
                                base.copy(staticCards = base.staticCards + newCard)
                            }
                            showAddDialog = false
                        },
                        enabled = dialogLabel.isNotBlank()
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
