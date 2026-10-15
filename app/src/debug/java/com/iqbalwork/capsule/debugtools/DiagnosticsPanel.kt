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

/** Debug-only simulators. They feed fake SystemEvents so cards can be checked before real sources exist. */
object DiagnosticsPanel {
    const val isAvailable = true

    private class Simulation(val title: String, val events: () -> List<SystemEvent>)

    private val simulations = listOf(
        Simulation("Headphones connected (mini event)") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.HEADPHONES_CONNECTED, "Pixel Buds Pro", "92%", 92)))
        },
        Simulation("Charger plugged in") {
            listOf(
                SystemEvent.ChargingUpdated(
                    ChargingState(isCharging = true, chargePercentage = 85, chargeType = ChargingType.AC, batteryTemperatureC = 32.1f, chargeTimeRemainingMs = 1_800_000L)
                ),
                SystemEvent.MiniEventStarted(miniEvent(MiniEventType.CHARGER_CONNECTED, "Charging", "85%", 85))
            )
        },
        Simulation("Charger unplugged") {
            listOf(
                SystemEvent.ChargingUpdated(ChargingState(isCharging = false, chargePercentage = 85)),
                SystemEvent.MiniEventStarted(miniEvent(MiniEventType.CHARGER_DISCONNECTED, "Charger removed", "85%", 85))
            )
        },
        Simulation("Low battery") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.LOW_BATTERY, "Low battery", "14%", 14)))
        },
        Simulation("Ringer: vibrate") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.RINGER_VIBRATE, "Vibrate")))
        },
        Simulation("Wi-Fi connected") {
            listOf(SystemEvent.MiniEventStarted(miniEvent(MiniEventType.WIFI_CONNECTED, "Wi-Fi connected")))
        },
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
    )

    @Composable
    private fun RealCard(title: String, subtitle: String, onClick: () -> Unit) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    private fun miniEvent(type: MiniEventType, title: String, subtitle: String = "", level: Int? = null) =
        MiniEvent(id = System.nanoTime(), type = type, title = title, subtitle = subtitle, level = level)

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
            item {
                Card(
                    onClick = { DebugNotifications.post(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Real chat notification", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Posts a MessagingStyle notification with reply and mark-as-read actions. " +
                                "Needs the notification permission for Capsool.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                RealCard("Real download (progress notification)", "Counts to 100% over ~6 s, then completes.") {
                    DebugOngoing.startDownload(context)
                }
            }
            item {
                RealCard("Real 2-minute timer (chronometer notification)", "+1:00, Pause/Resume and Stop buttons work.") {
                    DebugOngoing.startTimer(context)
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
