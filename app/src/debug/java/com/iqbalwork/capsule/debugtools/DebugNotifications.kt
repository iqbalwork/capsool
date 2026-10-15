package com.iqbalwork.capsule.debugtools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log

/**
 * Debug-only: posts real chat notifications so the listener → filter → card → reply path can be
 * tested without a messaging app. Replies and mark-as-read land in [DebugNotificationReceiver].
 */
object DebugNotifications {
    private const val CHANNEL = "capsool_debug_chat"
    const val KEY_REPLY = "reply_text"
    const val ACTION_REPLY = "com.iqbalwork.capsule.debug.REPLY"
    const val ACTION_READ = "com.iqbalwork.capsule.debug.READ"
    const val EXTRA_ID = "id"
    private val senders = listOf("Alex", "Sam", "Rina")
    private var count = 0

    fun post(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Debug chat", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val id = 1000 + (count % senders.size)
        val sender = senders[count % senders.size]
        count++

        val person = android.app.Person.Builder().setName(sender).build()
        val me = android.app.Person.Builder().setName("Me").build()
        val style = Notification.MessagingStyle(me)
            .addMessage("Are we still meeting today? (#$count)", System.currentTimeMillis(), person)

        val reply = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_send),
            "Reply",
            broadcast(context, ACTION_REPLY, id, mutable = true)
        )
            .addRemoteInput(RemoteInput.Builder(KEY_REPLY).setLabel("Reply").build())
            .setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY)
            .build()
        val read = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_view),
            "Mark as read",
            broadcast(context, ACTION_READ, id, mutable = false)
        )
            .setSemanticAction(Notification.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .build()

        manager.notify(
            id,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setStyle(style)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(0xFF34A853.toInt())
                .addAction(reply)
                .addAction(read)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun broadcast(context: Context, action: String, id: Int, mutable: Boolean): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName).putExtra(EXTRA_ID, id)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE)
        return PendingIntent.getBroadcast(context, id * 10 + action.length, intent, flags)
    }
}

class DebugNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(DebugNotifications.EXTRA_ID, 0)
        when (intent.action) {
            DebugNotifications.ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(DebugNotifications.KEY_REPLY)
                // Debug build only: logs the reply so the end-to-end test can be verified.
                Log.i("CapsoolDebug", "Reply received for $id: $text")
            }
            DebugNotifications.ACTION_READ -> Log.i("CapsoolDebug", "Marked read: $id")
        }
        context.getSystemService(NotificationManager::class.java).cancel(id)
    }
}
