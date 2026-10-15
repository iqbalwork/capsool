package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.ShortcutItem
import com.iqbalwork.capsule.model.ShortcutType
import com.iqbalwork.capsule.model.ShortcutsState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedShortcutsCard(
    shortcutsState: ShortcutsState,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Quick Shortcuts & Barcode",
                color = CapsuleTokens.OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${shortcutsState.shortcuts.size} actions",
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 10.sp
            )
        }

        // Grid of Shortcuts
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shortcutsState.shortcuts) { item ->
                ShortcutGridTile(
                    item = item,
                    onClick = { onAction(UserIntent.LaunchShortcut(item.id)) }
                )
            }
        }

        // Loyalty Card Barcode Simulator Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CapsuleTokens.SurfaceContainerHighest)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EAN-13: 8991234567890",
                color = CapsuleTokens.OnSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Barcode Stripes Canvas
            Canvas(modifier = Modifier.size(width = 80.dp, height = 16.dp)) {
                val stripeWidths = listOf(2f, 1f, 3f, 1f, 2f, 4f, 1f, 2f, 3f, 1f, 2f, 1f, 3f, 2f)
                var currentX = 0f
                stripeWidths.forEachIndexed { i, w ->
                    if (i % 2 == 0) {
                        drawLine(
                            color = Color.White,
                            start = Offset(currentX, 0f),
                            end = Offset(currentX, size.height),
                            strokeWidth = w
                        )
                    }
                    currentX += w + 2f
                }
            }
        }
    }
}

@Composable
fun ShortcutGridTile(
    item: ShortcutItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconEmoji = when (item.type) {
        ShortcutType.CHATGPT -> "🤖"
        ShortcutType.APP -> "💬"
        ShortcutType.SYSTEM -> if (item.id == "flashlight") "🔦" else if (item.id == "bluetooth") "ᛒ" else "📶"
        ShortcutType.CONTACT -> "👤"
        ShortcutType.BARCODE -> "💳"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CapsuleTokens.SurfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(CapsuleTokens.PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column {
            Text(
                text = item.title,
                color = CapsuleTokens.OnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    text = item.subtitle,
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
