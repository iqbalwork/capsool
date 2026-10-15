package com.iqbalwork.capsule.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.MediaState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens
import kotlinx.coroutines.delay

/** Colors for the media card, derived from the cover so each track gets its own tint. */
data class MediaPalette(val container: Color, val accent: Color, val onAccent: Color)

fun mediaPalette(seed: Color): MediaPalette {
    val accent = if (seed.luminance() < 0.3f) lerp(seed, Color.White, 0.45f) else seed
    return MediaPalette(
        container = lerp(Color.Black, seed, 0.3f),
        accent = accent,
        onAccent = if (accent.luminance() > 0.45f) Color.Black else Color.White
    )
}

@Composable
fun ExpandedMediaCard(
    mediaState: MediaState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(mediaState.dominantColor) { mediaPalette(Color(mediaState.dominantColor)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.container, Color.Black)))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(
                mediaState = mediaState,
                palette = palette,
                modifier = Modifier.clickable { onAction(UserIntent.MediaOpenApp) }
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaState.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (mediaState.artist.isNotEmpty()) {
                    Text(
                        text = mediaState.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (mediaState.isPlaying) {
                EqualizerWaveMini(color = palette.accent, modifier = Modifier.padding(start = 8.dp))
            }
        }

        // Players that do not support seeking (or report no duration) get no seek bar.
        if (mediaState.showsSeekBar) SeekBar(mediaState, palette, onAction)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(R.drawable.ic_skip_previous, "Previous", Color.White) { onAction(UserIntent.MediaPrevious) }
            PlayPauseButton(mediaState.isPlaying, palette) { onAction(UserIntent.MediaPlayPause) }
            ControlButton(R.drawable.ic_skip_next, "Next", Color.White) { onAction(UserIntent.MediaNext) }
        }
    }
}

@Composable
private fun Cover(mediaState: MediaState, palette: MediaPalette, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val artwork = mediaState.artwork
    if (artwork != null) {
        Image(
            bitmap = remember(artwork) { artwork.asImageBitmap() },
            contentDescription = "Open player",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(shape)
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mediaState.title.take(1).uppercase(),
                color = palette.onAccent,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun SeekBar(mediaState: MediaState, palette: MediaPalette, onAction: (UserIntent) -> Unit) {
    val now by produceState(SystemClock.elapsedRealtime(), mediaState) {
        while (mediaState.isPlaying) {
            value = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val position = mediaState.positionAt(now)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (position.toFloat() / mediaState.durationMs).coerceIn(0f, 1f)

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onAction(UserIntent.MediaSeek((it * mediaState.durationMs).toLong())) }
                dragFraction = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragFraction?.let { (it * mediaState.durationMs).toLong() } ?: position
            Text(formatTime(shown), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(formatTime(mediaState.durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

/** Play/pause morphs from a circle (paused) to a rounded square (playing), M3 Expressive style. */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, palette: MediaPalette, onClick: () -> Unit) {
    val corner by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 26.dp,
        animationSpec = CapsuleTokens.FruitSpringDp,
        label = "play_pause_shape"
    )
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 52.dp)
            .clip(RoundedCornerShape(corner))
            .background(palette.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = palette.onAccent,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ControlButton(icon: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
