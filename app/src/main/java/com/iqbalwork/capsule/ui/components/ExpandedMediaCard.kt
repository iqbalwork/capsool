package com.iqbalwork.capsule.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedMediaCard(
    mediaState: MediaState,
    onAction: (UserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "media_disc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Album Art + Title + Artist + Equalizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rotating Album Art Disc
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .rotate(if (mediaState.isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .background(Color(mediaState.dominantColor)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(CapsuleTokens.Surface)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaState.title,
                    color = CapsuleTokens.OnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mediaState.artist,
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            EqualizerWaveMini(modifier = Modifier.padding(end = 4.dp))
        }

        // Middle section: Scrubber Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            val progress = if (mediaState.durationMs > 0) {
                (mediaState.playbackPositionMs.toFloat() / mediaState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    val newMs = (newProgress * mediaState.durationMs).toLong()
                    onAction(UserAction.MediaSeek(newMs))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = CapsuleTokens.Primary,
                    activeTrackColor = CapsuleTokens.Primary,
                    inactiveTrackColor = CapsuleTokens.OutlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSecs = (mediaState.playbackPositionMs / 1000).toInt()
                val totalSecs = (mediaState.durationMs / 1000).toInt()
                Text(
                    text = "%d:%02d".format(currentSecs / 60, currentSecs % 60),
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "%d:%02d".format(totalSecs / 60, totalSecs % 60),
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        // Bottom section: Controls (Previous, Play/Pause, Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CapsuleTokens.SurfaceContainerHigh)
                    .clickable { onAction(UserAction.MediaPrevious) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏮",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Play / Pause Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CapsuleTokens.Primary)
                    .clickable { onAction(UserAction.MediaPlayPause) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mediaState.isPlaying) "⏸" else "▶",
                    color = CapsuleTokens.OnPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Next Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CapsuleTokens.SurfaceContainerHigh)
                    .clickable { onAction(UserAction.MediaNext) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏭",
                    color = CapsuleTokens.OnSurface,
                    fontSize = 16.sp
                )
            }
        }
    }
}
