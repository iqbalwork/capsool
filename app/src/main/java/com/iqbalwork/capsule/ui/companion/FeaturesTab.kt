package com.iqbalwork.capsule.ui.companion

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.state.CapsuleState
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun FeaturesTab(
    state: CapsuleState,
    onAction: (UserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Dynamic Cards Stack (Carousel)",
                color = CapsuleTokens.OnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enable or disable cards in the horizontal carousel stack. Swipe left or right on the island to cycle through active cards.",
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(CapsuleCardType.entries) { cardType ->
            val isEnabled = state.availableCards.contains(cardType)
            val isCurrentActive = state.activeCard == cardType

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CapsuleTokens.SurfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val iconEmoji = when (cardType) {
                        CapsuleCardType.MEDIA -> "🎵"
                        CapsuleCardType.SLIDERS -> "🔊"
                        CapsuleCardType.CHARGING -> "⚡"
                        CapsuleCardType.NOTIFICATION -> "💬"
                        CapsuleCardType.TIMER -> "⏱️"
                        CapsuleCardType.SHORTCUTS -> "⚡"
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentActive) CapsuleTokens.Primary else CapsuleTokens.SurfaceContainerHighest
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = iconEmoji, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = cardType.displayName,
                            color = CapsuleTokens.OnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCurrentActive) "Active in capsule" else if (isEnabled) "In carousel" else "Disabled",
                            color = if (isCurrentActive) CapsuleTokens.Primary else CapsuleTokens.OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CapsuleTokens.PrimaryContainer)
                                .clickable { onAction(UserAction.SelectCard(cardType)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Preview",
                                color = CapsuleTokens.OnSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onAction(UserAction.ToggleCardEnabled(cardType, it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CapsuleTokens.Primary,
                            checkedTrackColor = CapsuleTokens.PrimaryContainer,
                            uncheckedThumbColor = CapsuleTokens.Outline,
                            uncheckedTrackColor = CapsuleTokens.SurfaceContainerHighest
                        )
                    )
                }
            }
        }
    }
}
