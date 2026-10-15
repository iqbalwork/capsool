package com.iqbalwork.capsule.platform.notification

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.iqbalwork.capsule.appGraph

/** Notification access: active media sessions (M1) and notifications in the capsule (M2). */
class CapsoolNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        appGraph.notificationSource.attach(this)
        appGraph.onNotificationListenerConnected(true)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        appGraph.notificationSource.onPosted(sbn, rankingMap)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        appGraph.notificationSource.onRemoved(sbn)
    }

    override fun onListenerDisconnected() {
        appGraph.notificationSource.detach()
        appGraph.onNotificationListenerConnected(false)
        // Ask to be bound again; this is a no-op when the user has revoked access.
        requestRebind(ComponentName(this, CapsoolNotificationListener::class.java))
        super.onListenerDisconnected()
    }
}
