package com.iqbalwork.capsule.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon names a custom intent can store (F-76). Lives here rather than in the companion tab so
 * the overlay card and the editor share one mapping instead of drifting apart.
 */
val AvailableIntentIcons = listOf(
    "bolt", "star", "play", "settings", "notifications",
    "send", "power", "refresh", "music", "camera", "phone", "message", "share", "flash"
)

/** Unknown names fall back to the bolt so a hand-edited setting cannot crash the card. */
fun customIntentIcon(name: String): ImageVector = when (name.lowercase()) {
    "bolt" -> Icons.Rounded.Bolt
    "star" -> Icons.Rounded.Star
    "play", "play_arrow" -> Icons.Rounded.PlayArrow
    "settings" -> Icons.Rounded.Settings
    "notifications" -> Icons.Rounded.Notifications
    "send" -> Icons.AutoMirrored.Rounded.Send
    "power", "power_settings_new" -> Icons.Rounded.PowerSettingsNew
    "refresh" -> Icons.Rounded.Refresh
    "music", "music_note" -> Icons.Rounded.MusicNote
    "camera" -> Icons.Rounded.Camera
    "phone" -> Icons.Rounded.Phone
    "message" -> Icons.AutoMirrored.Rounded.Message
    "share" -> Icons.Rounded.Share
    "flash", "flashlight" -> Icons.Rounded.FlashlightOn
    else -> Icons.Rounded.Bolt
}
