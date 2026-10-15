package com.iqbalwork.capsule

import com.iqbalwork.capsule.model.CapsuleNotification

object NotificationFixtures {
    fun notification(
        key: String,
        canReply: Boolean = true,
        clearable: Boolean = true,
        isPublic: Boolean = false
    ) = CapsuleNotification(
        key = key,
        packageName = "com.chat",
        appName = "Chat",
        sender = "Alex",
        text = "Hi $key",
        glowColor = 0xFF34A853,
        postedAt = 0L,
        canReply = canReply,
        canMarkRead = true,
        clearable = clearable,
        isPublic = isPublic
    )
}
