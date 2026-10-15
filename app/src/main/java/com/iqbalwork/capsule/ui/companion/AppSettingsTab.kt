package com.iqbalwork.capsule.ui.companion

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.state.CapsuleState
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.ChargingState
import com.iqbalwork.capsule.model.ChargingType
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.NotificationActionItem
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun AppSettingsTab(
    state: CapsuleState,
    onAction: (UserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Service & Diagnostics",
                color = CapsuleTokens.OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage Accessibility Overlay permissions and test real-time event simulations.",
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // 1. Accessibility Service Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Accessibility Service Overlay",
                            color = CapsuleTokens.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uses TYPE_ACCESSIBILITY_OVERLAY to draw the capsule directly without SYSTEM_ALERT_WINDOW.",
                            color = CapsuleTokens.OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CapsuleTokens.Primary)
                            .clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Settings",
                            color = CapsuleTokens.OnPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Real-Time Diagnostics Panel (Milestone 3 requirement)
        item {
            Text(
                text = "Diagnostics Simulation Panel",
                color = CapsuleTokens.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TWS Simulation
                DiagnosticButton(
                    title = "Simulate TWS Connection",
                    subtitle = "AirPods Pro (92% Battery)",
                    iconEmoji = "🎧"
                ) {
                    onAction(
                        UserAction.TriggerMiniEvent(
                            MiniEvent(
                                type = MiniEventType.TWS_CONNECTED,
                                title = "AirPods Pro",
                                subtitle = "• 92%",
                                durationMs = 3500L
                            )
                        )
                    )
                }

                // Charger Connected Simulation
                DiagnosticButton(
                    title = "Simulate AC Charger Plugged",
                    subtitle = "85% • AC Fast Charging",
                    iconEmoji = "⚡"
                ) {
                    onAction(
                        UserAction.UpdateCharging(
                            ChargingState(
                                isCharging = true,
                                chargePercentage = 85,
                                chargeType = ChargingType.AC,
                                batteryTemperatureC = 32.1f
                            )
                        )
                    )
                    onAction(
                        UserAction.TriggerMiniEvent(
                            MiniEvent(
                                type = MiniEventType.CHARGER_CONNECTED,
                                title = "Charging",
                                subtitle = "85% AC",
                                durationMs = 3000L
                            )
                        )
                    )
                }

                // Low Battery Simulation
                DiagnosticButton(
                    title = "Simulate Low Battery Alert",
                    subtitle = "4% Battery Warning",
                    iconEmoji = "⚠️"
                ) {
                    onAction(
                        UserAction.UpdateCharging(
                            ChargingState(
                                isCharging = false,
                                chargePercentage = 4,
                                chargeType = ChargingType.NONE,
                                isLowBattery = true
                            )
                        )
                    )
                    onAction(
                        UserAction.TriggerMiniEvent(
                            MiniEvent(
                                type = MiniEventType.LOW_BATTERY,
                                title = "Low Battery",
                                subtitle = "4% remaining",
                                durationMs = 3500L
                            )
                        )
                    )
                }

                // WhatsApp Notification Simulation
                DiagnosticButton(
                    title = "Simulate WhatsApp Message",
                    subtitle = "Alex: Hey, are we still meeting today?",
                    iconEmoji = "💬"
                ) {
                    onAction(
                        UserAction.UpdateNotification(
                            NotificationState(
                                id = "test_${System.currentTimeMillis()}",
                                title = "WhatsApp",
                                content = "Alex: Hey, are we still meeting today?",
                                packageName = "com.whatsapp",
                                appName = "WhatsApp",
                                glowColor = 0xFF25D366,
                                actions = listOf(
                                    NotificationActionItem("reply", "Reply", isReply = true),
                                    NotificationActionItem("read", "Mark as read"),
                                    NotificationActionItem("dismiss", "Dismiss")
                                )
                            )
                        )
                    )
                }

                // Media Playback Simulation
                DiagnosticButton(
                    title = "Simulate Media Playback",
                    subtitle = "Blinding Lights • The Weeknd",
                    iconEmoji = "🎵"
                ) {
                    onAction(
                        UserAction.UpdateMedia(
                            MediaState(
                                title = "Blinding Lights",
                                artist = "The Weeknd",
                                isPlaying = true,
                                playbackPositionMs = 45000L,
                                durationMs = 200000L,
                                dominantColor = 0xFF8E24AA
                            )
                        )
                    )
                }

                // Active Timer Simulation
                DiagnosticButton(
                    title = "Simulate Active Countdown Timer",
                    subtitle = "Work Sprint (04:05)",
                    iconEmoji = "⏱️"
                ) {
                    onAction(
                        UserAction.UpdateTimer(
                            TimerState(
                                remainingSeconds = 245,
                                totalSeconds = 300,
                                isRunning = true,
                                label = "Work Sprint"
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticButton(
    title: String,
    subtitle: String,
    iconEmoji: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CapsuleTokens.SurfaceContainerHighest)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CapsuleTokens.PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CapsuleTokens.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 10.sp
            )
        }

        Text(
            text = "Trigger",
            color = CapsuleTokens.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
