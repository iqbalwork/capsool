package com.iqbalwork.capsule.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.iqbalwork.capsule.platform.Oem
import com.iqbalwork.capsule.platform.OemGuides
import com.iqbalwork.capsule.platform.SystemStatus
import com.iqbalwork.capsule.platform.detectOem

@Composable
fun StatusTab(serviceRunning: Boolean, notificationAccess: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var advancedProtection by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        advancedProtection = SystemStatus.isAdvancedProtectionEnabled(context)
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatusCard(
                title = "Accessibility service",
                body = if (serviceRunning) {
                    "Running. The capsule is drawn over other apps."
                } else {
                    "Off. Turn on Capsool in Accessibility settings to show the capsule."
                },
                ok = serviceRunning,
                action = if (serviceRunning) null else "Open Accessibility settings" to {
                    context.startActivity(SystemStatus.accessibilitySettingsIntent())
                }
            )
        }
        item {
            StatusCard(
                title = "Notification access",
                body = if (notificationAccess) {
                    "On. The capsule can follow music and video players."
                } else {
                    "Off. Needed to show what is playing and control it from the capsule. " +
                        "Capsool reads notifications on your phone only and never stores their text."
                },
                ok = notificationAccess,
                action = if (notificationAccess) null else "Allow notification access" to {
                    SystemStatus.openNotificationAccess(context)
                }
            )
        }
        if (!serviceRunning || !notificationAccess) {
            item {
                StatusCard(
                    title = "Option greyed out?",
                    body = "On Android 13+ apps installed outside the Play Store are blocked from Accessibility " +
                        "and notification access. " +
                        "Open App info, tap ⋮ and choose \"Allow restricted settings\", then try again.",
                    ok = null,
                    action = "Open App info" to { context.startActivity(SystemStatus.appDetailsIntent(context)) }
                )
            }
        }
        item { KeepAliveCard() }
        if (advancedProtection) {
            item {
                StatusCard(
                    title = "Advanced Protection is on",
                    body = "Android Advanced Protection can switch off accessibility services that are not " +
                        "assistive tools, which stops the capsule. Capsool cannot work around this.",
                    ok = false,
                    action = null
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    ok: Boolean?,
    action: Pair<String, () -> Unit>?
) {
    val accent = when (ok) {
        true -> Color(0xFF4CAF50)
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = accent)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action?.let { (label, onClick) -> FilledTonalButton(onClick = onClick) { Text(label) } }
        }
    }
}

/** F-92: how to stop the phone's battery manager from killing the capsule, for the detected brand. */
@Composable
private fun KeepAliveCard() {
    val context = LocalContext.current
    val detected = remember { detectOem(Build.MANUFACTURER, Build.BRAND) }
    var selected by remember { mutableStateOf(detected) }
    val guide = OemGuides.guideFor(selected)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Keep the capsule running", style = MaterialTheme.typography.titleMedium)
            Text(
                "Some phones stop background apps to save battery, which makes the capsule disappear. " +
                    "Follow these steps once.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Oem.entries.forEach { oem ->
                    FilterChip(
                        selected = oem == selected,
                        onClick = { selected = oem },
                        label = { Text(if (oem == detected) "${oem.displayName} (this phone)" else oem.displayName) }
                    )
                }
            }
            guide.steps.forEachIndexed { index, step ->
                val isNote = index == guide.steps.lastIndex
                Text(
                    if (isNote) step else "${index + 1}. $step",
                    style = if (isNote) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = if (isNote) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { context.startActivity(SystemStatus.appDetailsIntent(context)) }) {
                    Text("App info")
                }
                FilledTonalButton(onClick = {
                    runCatching { context.startActivity(SystemStatus.batteryOptimizationIntent()) }
                }) { Text("Battery list") }
                if (selected == Oem.XIAOMI) {
                    FilledTonalButton(onClick = {
                        if (!SystemStatus.openXiaomiAutostart(context)) {
                            context.startActivity(SystemStatus.appDetailsIntent(context))
                        }
                    }) { Text("Autostart") }
                }
            }
        }
    }
}
