package com.iqbalwork.capsule.ui.companion

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * F-22 and F-26: choose which apps may show notifications in the capsule, and jump to each
 * app's notification settings to turn off its system pop-up so it does not show twice.
 */
@Composable
fun NotificationAppsScreen(
    settings: Settings,
    onUpdate: ((Settings) -> Settings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val apps by produceState<List<LaunchableApp>?>(null) {
        value = withContext(Dispatchers.Default) { LaunchableApps.load(context) }
    }
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Apps in the capsule", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Switch off apps you do not want in the capsule. For apps you keep, tap \"Pop-up\" and turn off " +
                "the system pop-up so their messages do not show twice.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val list = apps
        if (list == null) {
            Text("Loading apps…", style = MaterialTheme.typography.bodyMedium)
        } else {
            val shown = list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(shown, key = { it.packageName }) { app ->
                    val allowed = app.packageName !in settings.notificationBlockedApps
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { context.startActivity(popUpSettings(app.packageName)) }) { Text("Pop-up") }
                        Switch(
                            checked = allowed,
                            onCheckedChange = { allow ->
                                onUpdate {
                                    val blocked = if (allow) it.notificationBlockedApps - app.packageName
                                    else it.notificationBlockedApps + app.packageName
                                    it.copy(notificationBlockedApps = blocked)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun popUpSettings(packageName: String): Intent =
    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
