package com.iqbalwork.capsule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iqbalwork.capsule.engine.state.UserAction
import com.iqbalwork.capsule.model.NotificationState
import com.iqbalwork.capsule.ui.theme.CapsuleTokens

@Composable
fun ExpandedNotificationCard(
    notificationState: NotificationState?,
    onAction: (UserAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (notificationState == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active notifications",
                color = CapsuleTokens.OnSurfaceVariant,
                fontSize = 13.sp
            )
        }
        return
    }

    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val appBadgeColor = if (notificationState.packageName.contains("whatsapp", ignoreCase = true)) {
        CapsuleTokens.WhatsAppBadge
    } else if (notificationState.packageName.contains("telegram", ignoreCase = true)) {
        CapsuleTokens.TelegramBadge
    } else {
        Color(notificationState.glowColor)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Avatar (42dp) + App badge + Sender & Message
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar (42dp) with badge overlay
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CapsuleTokens.SurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificationState.title.take(1).uppercase(),
                        color = CapsuleTokens.OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // App Badge Dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(appBadgeColor)
                        .border(1.5.dp, CapsuleTokens.Surface, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notificationState.title,
                        color = CapsuleTokens.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Now",
                        color = CapsuleTokens.OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notificationState.content,
                    color = CapsuleTokens.OnSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Inline Reply Field or Quick Actions
        if (isReplying) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text(text = "Type a reply...", fontSize = 11.sp, color = CapsuleTokens.Outline)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CapsuleTokens.Primary,
                        unfocusedBorderColor = CapsuleTokens.OutlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CapsuleTokens.Primary)
                        .clickable {
                            if (replyText.isNotBlank()) {
                                onAction(UserAction.ReplyNotification(notificationState.id, replyText))
                                isReplying = false
                                replyText = ""
                            }
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Send",
                        color = CapsuleTokens.OnPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reply Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CapsuleTokens.PrimaryContainer)
                        .clickable { isReplying = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reply",
                        color = CapsuleTokens.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mark as Read
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CapsuleTokens.SurfaceContainerHigh)
                        .clickable {
                            onAction(UserAction.DismissNotification(notificationState.id))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mark as read",
                        color = CapsuleTokens.OnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Dismiss
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CapsuleTokens.SurfaceContainerHighest)
                        .clickable {
                            onAction(UserAction.DismissNotification(notificationState.id))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dismiss",
                        color = CapsuleTokens.OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
