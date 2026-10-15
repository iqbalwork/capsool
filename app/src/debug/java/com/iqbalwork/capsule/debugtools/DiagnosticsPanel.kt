package com.iqbalwork.capsule.debugtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.core.store.Action
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.NotificationActionItem
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.TimerState

/** Debug-only simulators. They feed fake SystemEvents so cards can be checked before real sources exist. */
object DiagnosticsPanel {
    const val isAvailable = true

    private class Simulation(val title: String, val events: () -> List<SystemEvent>)

    private val simulations = listOf(
        Simulation("Headphones connected (mini event)") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.TWS_CONNECTED, "Headphones", "92%")))
        },
        Simulation("Charger plugged in") {
            listOf(
                SystemEvent.ChargingUpdated(
                    ChargingState(isCharging = true, chargePercentage = 85, chargeType = ChargingType.AC, batteryTemperatureC = 32.1f)
                ),
                SystemEvent.MiniEventStarted(miniEvent(MiniEventType.CHARGER_CONNECTED, "Charging", "85%"))
            )
        },
        Simulation("Charger unplugged") {
            listOf(SystemEvent.ChargingUpdated(ChargingState(isCharging = false, chargePercentage = 85)))
        },
        Simulation("Low battery") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.LOW_BATTERY, "Low battery", "4%")))
        },
        Simulation("Chat message") {
            listOf(
                SystemEvent.NotificationUpdated(
                    NotificationState(
                        id = "debug_${System.currentTimeMillis()}",
                        title = "Alex",
                        content = "Are we still meeting today?",
                        packageName = "com.example.chat",
                        appName = "Chat",
                        timestamp = System.currentTimeMillis(),
                        glowColor = 0xFF4CAF50,
                        actions = listOf(
                            NotificationActionItem("reply", "Reply", isReply = true),
                            NotificationActionItem("dismiss", "Dismiss")
                        )
                    )
                )
            )
        },
        Simulation("Clear notification") { listOf(SystemEvent.NotificationUpdated(null)) },
        Simulation("Media playing") {
            listOf(
                SystemEvent.MediaUpdated(
                    MediaState(
                        title = "Test Track",
                        artist = "Test Artist",
                        isPlaying = true,
                        playbackPositionMs = 45_000L,
                        durationMs = 200_000L,
                        dominantColor = 0xFF8E24AA
                    )
                )
            )
        },
        Simulation("Stop media") { listOf(SystemEvent.MediaUpdated(null)) },
        Simulation("Timer running") {
            listOf(SystemEvent.TimerUpdated(TimerState(remainingSeconds = 245, totalSeconds = 300, isRunning = true, label = "Timer")))
        },
        Simulation("Clear timer") { listOf(SystemEvent.TimerUpdated(null)) }
    )

    private fun miniEvent(type: MiniEventType, title: String, subtitle: String) =
        MiniEvent(id = System.nanoTime(), type = type, title = title, subtitle = subtitle)

    @Composable
    fun Content(dispatch: (Action) -> Unit) {
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Debug build only. These inject fake events; controls on simulated cards do not reach the system.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                Card(
                    onClick = { DebugMediaSession.toggle(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Real media session (start / stop)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Goes through notification access and MediaSessionManager like a real player.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            items(simulations) { simulation ->
                Card(
                    onClick = { simulation.events().forEach(dispatch) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(simulation.title, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
