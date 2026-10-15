package com.iqbalwork.capsule.debugtools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper

/**
 * Debug-only: real ongoing notifications (a download and a clock-style timer) so progress and
 * timer cards can be tested end to end. Timer buttons land in [DebugTimerReceiver].
 */
object DebugOngoing {
    private const val CHANNEL = "capsool_debug_ongoing"
    private const val DOWNLOAD_ID = 2000
    const val TIMER_ID = 2001
    const val ACTION_ADD = "com.iqbalwork.capsule.debug.TIMER_ADD"
    const val ACTION_PAUSE = "com.iqbalwork.capsule.debug.TIMER_PAUSE"
    const val ACTION_STOP = "com.iqbalwork.capsule.debug.TIMER_STOP"
    private val handler = Handler(Looper.getMainLooper())

    // Timer state: end time while running, remaining millis while paused.
    private var endAt = 0L
    private var pausedRemaining: Long? = null

    fun startDownload(context: Context) {
        val manager = channel(context)
        var percent = 0
        fun step() {
            if (percent < 100) {
                manager.notify(DOWNLOAD_ID, base(context).setContentTitle("debug-file.zip").setOngoing(true)
                    .setSmallIcon(android.R.drawable.stat_sys_download).setProgress(100, percent, false).build())
                percent += 5
                handler.postDelayed(::step, 300)
            } else {
                manager.cancel(DOWNLOAD_ID)
            }
        }
        step()
    }

    fun startTimer(context: Context) {
        endAt = System.currentTimeMillis() + 120_000
        pausedRemaining = null
        postTimer(context)
    }

    fun onAction(context: Context, action: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        when (action) {
            ACTION_ADD -> {
                pausedRemaining = pausedRemaining?.plus(60_000)
                endAt += 60_000
            }
            ACTION_PAUSE -> pausedRemaining = if (pausedRemaining == null) {
                (endAt - System.currentTimeMillis()).coerceAtLeast(0)
            } else {
                endAt = System.currentTimeMillis() + pausedRemaining!!
                null
            }
            ACTION_STOP -> {
                manager.cancel(TIMER_ID)
                return
            }
        }
        postTimer(context)
    }

    private fun postTimer(context: Context) {
        val manager = channel(context)
        val paused = pausedRemaining
        val builder = base(context)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .addAction(action(context, "+1:00", ACTION_ADD))
            .addAction(action(context, if (paused == null) "Pause" else "Resume", ACTION_PAUSE))
            .addAction(action(context, "Stop", ACTION_STOP))
        if (paused == null) {
            builder.setWhen(endAt).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
        } else {
            val seconds = paused / 1000
            builder.setContentText("%d:%02d".format(seconds / 60, seconds % 60)).setUsesChronometer(false)
        }
        manager.notify(TIMER_ID, builder.build())
    }

    private fun action(context: Context, title: String, intentAction: String): Notification.Action {
        val intent = Intent(intentAction).setPackage(context.packageName)
        val pending = PendingIntent.getBroadcast(context, intentAction.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(Icon.createWithResource(context, android.R.drawable.ic_media_pause), title, pending).build()
    }

    private fun base(context: Context) = Notification.Builder(context, CHANNEL).setOnlyAlertOnce(true)

    private fun channel(context: Context): NotificationManager {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Debug ongoing", NotificationManager.IMPORTANCE_LOW))
        return manager
    }
}

class DebugTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = DebugOngoing.onAction(context, intent.action)
}
