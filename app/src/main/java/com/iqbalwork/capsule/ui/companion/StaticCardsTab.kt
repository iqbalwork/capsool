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
import androidx.compose.ui.res.stringResource
import com.iqbalwork.capsule.R
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
                    Text(stringResource(R.string.sc_view_mode), style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        StaticCardViewMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.staticCardViewMode == mode,
                                onClick = { onUpdate { it.copy(staticCardViewMode = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(index, StaticCardViewMode.entries.size)
                            ) {
                                Text(stringResource(if (mode == StaticCardViewMode.CAROUSEL) R.string.sc_carousel else R.string.sc_list))
                            }
                        }
                    }
                    Text(
                        if (settings.staticCardViewMode == StaticCardViewMode.CAROUSEL) {
                            stringResource(R.string.sc_carousel_desc)
                        } else {
                            stringResource(R.string.sc_list_desc)
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
                        Text(stringResource(R.string.sc_auto_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.sc_auto_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = { onUpdate { it.seedStaticCards() } },
                            enabled = displayCards.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.sc_edit_cards))
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
                            Text(stringResource(R.string.sc_cards), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.sc_cards_desc),
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
                                Text(stringResource(R.string.sc_add_card))
                            }
                            DropdownMenu(
                                expanded = addMenuExpanded,
                                onDismissRequest = { addMenuExpanded = false }
                            ) {
                                availableTypes.forEach { type ->
                                    val typeName = stringResource(type.labelRes)
                                    DropdownMenuItem(
                                        text = { Text(typeName) },
                                        onClick = {
                                            addMenuExpanded = false
                                            dialogType = type
                                            dialogLabel = typeName
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
                            stringResource(R.string.sc_empty),
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
                                Text(
                                    if (card.label == card.type.label) stringResource(card.type.labelRes) else card.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (isStart) {
                                    Text(
                                        stringResource(R.string.sc_start_marker),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            // The type only adds information once the card has been renamed.
                            if (card.label != card.type.label) {
                                Text(
                                    stringResource(card.type.labelRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isDerived) {
                            Text(
                                stringResource(R.string.sc_automatic),
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
                                    contentDescription = stringResource(if (isStart) R.string.sc_unset_start else R.string.sc_set_start),
                                    tint = if (isStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onUpdate { s -> s.copy(staticCards = s.staticCards.moveUp(card.id)) } },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.sc_move_up))
                            }
                            IconButton(
                                onClick = { onUpdate { s -> s.copy(staticCards = s.staticCards.moveDown(card.id)) } },
                                enabled = index < displayCards.lastIndex
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.sc_move_down))
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
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.sc_remove))
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
            val defaultName = stringResource(type.labelRes)
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.sc_add_card_title, stringResource(type.labelRes))) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialogLabel,
                            onValueChange = { dialogLabel = it },
                            label = { Text(stringResource(R.string.sc_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = {
                            // An untouched default is stored as the stable English key, so it keeps following the app
                            // language; only a name the user typed is stored as written.
                            val typed = dialogLabel.trim()
                            val label = if (typed.isEmpty() || typed == defaultName) type.label else typed
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
                        Text(stringResource(R.string.sc_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.sc_cancel))
                    }
                }
            )
        }
    }
}
