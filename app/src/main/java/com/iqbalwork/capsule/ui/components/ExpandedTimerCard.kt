package com.iqbalwork.capsule.ui.components

import com.iqbalwork.capsule.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.model.formatClock
import kotlinx.coroutines.delay

val TimerAmber = Color(0xFFFFB74D)

/**
 * Wall-clock time that ticks on each second boundary while [running]; frozen otherwise, so a
 * paused timer costs no frames. Only composed while the card or pill is on screen.
 */
@Composable
fun rememberTicker(running: Boolean): State<Long> = produceState(System.currentTimeMillis(), running) {
    while (running) {
        value = System.currentTimeMillis()
        delay(1_000 - value % 1_000)
    }
}

/** The text a timer shows right now: live while running, the app's own text while paused. */
fun TimerState.displayText(nowMs: Long): String =
    if (isRunning || staticText.isBlank()) formatClock(displaySeconds(nowMs)) else staticText

/** F-41: big live time and the clock app's own buttons (+1 min, pause, stop…). */
@Composable
fun ExpandedTimerCard(
    timer: TimerState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val now by rememberTicker(timer.isRunning)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Color.Black, TimerAmber, 0.18f), Color.Black)))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (timer.isCountdown) Icons.Rounded.Timer else Icons.Rounded.AvTimer,
                contentDescription = null,
                tint = TimerAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = timer.label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (timer.isRunning) timer.appName else stringResource(R.string.st_paused),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = timer.displayText(now),
            color = if (timer.isRunning) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 44.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")
        )
        if (timer.actions.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                timer.actions.take(3).forEach { action ->
                    FilledTonalButton(
                        onClick = { onAction(UserIntent.TimerAction(timer.key, action.index)) },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(action.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
