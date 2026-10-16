package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.Image
import com.iqbalwork.capsule.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.model.ProgressItem

private const val MAX_ROWS = 3

/** F-40: up to three downloads/uploads with their app, title and progress. */
@Composable
fun ExpandedProgressCard(items: List<ProgressItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        items.take(MAX_ROWS).forEach { item -> ProgressRow(item) }
        if (items.size > MAX_ROWS) {
            Text(
                stringResource(R.string.st_progress_more, items.size - MAX_ROWS),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ProgressRow(item: ProgressItem) {
    val accent = mediaPalette(Color(item.color)).accent
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIconBadge(item, accent, size = 32)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                item.fraction?.let {
                    Text(
                        "${(it * 100).toInt()}%",
                        color = accent,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                    )
                }
            }
            val fraction = item.fraction
            if (fraction != null) {
                LinearProgressIndicator(
                    progress = { fraction },
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(color = accent, trackColor = Color.White.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The posting app's status icon in a tinted circle; a download glyph if it has none. */
@Composable
fun AppIconBadge(item: ProgressItem, accent: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        val icon = item.appIcon
        if (icon != null) {
            Image(
                bitmap = remember(icon) { icon.asImageBitmap() },
                contentDescription = item.appName,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier.size((size * 0.55f).dp)
            )
        } else {
            Icon(Icons.Rounded.Downloading, contentDescription = item.appName, tint = accent, modifier = Modifier.size((size * 0.6f).dp))
        }
    }
}
