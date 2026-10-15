package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedTimerCard(
    timerState: TimerState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = timerState.remainingSeconds / 60
    val secs = timerState.remainingSeconds % 60
    val progress = if (timerState.totalSeconds > 0) {
        (timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏱️ ${timerState.label}",
                color = CapsuleTokens.OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CapsuleTokens.SurfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Google Clock Sync",
                    color = CapsuleTokens.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Center Countdown Display & Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "%02d:%02d".format(mins, secs),
                color = CapsuleTokens.Primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CapsuleTokens.SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .background(CapsuleTokens.Primary)
                )
            }
        }

        // Bottom Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // +1 Minute
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CapsuleTokens.SurfaceContainerHigh)
                    .clickable { onAction(UserIntent.TimerAddMinute) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+1 min",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Pause / Resume
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CapsuleTokens.Primary)
                    .clickable { onAction(UserIntent.TimerTogglePause) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (timerState.isRunning) "Pause" else "Resume",
                    color = CapsuleTokens.OnPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Reset
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CapsuleTokens.SurfaceContainerHighest)
                    .clickable { onAction(UserIntent.TimerReset) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reset",
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
