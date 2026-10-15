package com.iqbalwork.capsule.platform

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build

object PendingIntents {
    /**
     * Opening an app from the overlay (a notification, the player) is a background activity
     * start on Android 14+, so the sender has to opt in explicitly. Older versions need nothing.
     */
    fun sendLaunching(context: Context, intent: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.send()
            return
        }
        val options = ActivityOptions.makeBasic()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS)
        } else {
            @Suppress("DEPRECATION")
            options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
        }
        intent.send(context, 0, null, null, null, null, options.toBundle())
    }
}
