package com.iqbalwork.capsule.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleNotification

/**
 * The open notification card (F-20, F-21). Buttons appear only for what the posting app
 * supports; on the lock screen with text hidden, only the app name is shown.
 */
@Composable
fun ExpandedNotificationCard(
    notification: CapsuleNotification,
    showText: Boolean,
    replying: Boolean,
    queuedCount: Int,
    onAction: (UserIntent) -> Unit,
    modifier: Modifier = Modifier,
    /** F-23: drag the card sideways to dismiss it. Only for notifications the user may clear. */
    swipeToDismiss: Boolean = false
) {
    val accent = remember(notification.glowColor) { mediaPalette(Color(notification.glowColor)).accent }

    Column(
        modifier = modifier
            .fillMaxSize()
            .swipeToDismiss(
                enabled = swipeToDismiss && notification.clearable && !replying,
                key = notification.key,
                onDismiss = { onAction(UserIntent.NotificationDismiss(notification.key)) }
            )
            .background(Brush.verticalGradient(listOf(lerp(Color.Black, Color(notification.glowColor), 0.22f), Color.Black)))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, enabled = !replying) { onAction(UserIntent.NotificationOpen(notification.key)) },
            verticalAlignment = Alignment.Top
        ) {
            AvatarWithBadge(notification, size = 48.dp, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (showText) notification.sender else notification.appName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    val timeText = relativeTime(notification.postedAt)
                    Text(
                        text = buildString {
                            append(timeText)
                            if (queuedCount > 1) append(" · +${queuedCount - 1}")
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (showText) {
                    Text(
                        text = listOfNotNull(notification.appName, notification.conversationTitle).joinToString(" · "),
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (showText) notification.text else stringResource(R.string.st_notification_new),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (replying) {
            ReplyField(accent = accent, onSend = { onAction(UserIntent.NotificationReply(notification.key, it)) })
        } else {
            // Equal-width buttons so three of them fit the card without wrapping.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (notification.canReply && showText) {
                    CardButton(stringResource(R.string.st_reply), Modifier.weight(1f), accent, Color.Black) {
                        onAction(UserIntent.NotificationStartReply(notification.key))
                    }
                }
                if (notification.canMarkRead) {
                    CardButton(stringResource(R.string.st_read), Modifier.weight(1f)) { onAction(UserIntent.NotificationMarkRead(notification.key)) }
                }
                if (notification.clearable) {
                    CardButton(stringResource(R.string.st_dismiss), Modifier.weight(1f)) { onAction(UserIntent.NotificationDismiss(notification.key)) }
                }
            }
        }
    }
}

@Composable
private fun ReplyField(accent: Color, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // The window becomes focusable a frame after reply mode starts; ask for focus once composed.
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    fun send() {
        if (text.isNotBlank()) onSend(text.trim())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.st_message)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.12f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focus)
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = ::send,
            enabled = text.isNotBlank(),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.st_send))
        }
    }
}

@Composable
private fun CardButton(
    label: String,
    modifier: Modifier,
    container: Color = Color.White.copy(alpha = 0.12f),
    content: Color = Color.White,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Sender avatar (or initial) with the posting app's icon as a small badge. */
@Composable
fun AvatarWithBadge(notification: CapsuleNotification, size: Dp, accent: Color) {
    Box(modifier = Modifier.size(size)) {
        RoundBitmap(
            bitmap = notification.avatar,
            fallbackText = notification.sender,
            background = accent,
            modifier = Modifier.size(size)
        )
        val badge = size * 0.42f
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(badge)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            notification.appIcon?.let { icon ->
                Image(
                    bitmap = remember(icon) { icon.asImageBitmap() },
                    contentDescription = notification.appName,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier.size(badge * 0.65f)
                )
            }
        }
    }
}

@Composable
fun RoundBitmap(bitmap: Bitmap?, fallbackText: String, background: Color, modifier: Modifier = Modifier) {
    if (bitmap != null) {
        Image(
            bitmap = remember(bitmap) { bitmap.asImageBitmap() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.take(1).uppercase(),
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun relativeTime(postedAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = (now - postedAt) / 60_000
    return when {
        minutes < 1 -> stringResource(R.string.st_time_now)
        minutes < 60 -> stringResource(R.string.st_time_minutes, minutes)
        else -> stringResource(R.string.st_time_hours, minutes / 60)
    }
}
