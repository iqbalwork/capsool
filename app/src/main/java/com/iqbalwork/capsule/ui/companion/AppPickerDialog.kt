package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.iqbalwork.capsule.R
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Searchable list of launchable apps; used for app shortcuts and "Open app" gestures. */
@Composable
fun AppPickerDialog(title: String, onPick: (LaunchableApp) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps by produceState<List<LaunchableApp>?>(null) {
        value = withContext(Dispatchers.Default) { LaunchableApps.load(context) }
    }
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.sc_search_apps)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val list = apps
                if (list == null) {
                    Text(stringResource(R.string.sc_loading_apps), modifier = Modifier.padding(top = 12.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }, key = { it.packageName }) { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app) }
                                    .padding(vertical = 8.dp)
                            ) {
                                Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sc_cancel)) } }
    )
}
