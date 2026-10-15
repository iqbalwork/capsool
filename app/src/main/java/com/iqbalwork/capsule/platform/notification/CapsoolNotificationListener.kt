package com.iqbalwork.capsule.platform.notification

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import com.iqbalwork.capsule.appGraph

/**
 * Notification access is what lets Capsool read active media sessions (M1). Showing
 * notifications in the capsule arrives in M2; until then posted notifications are ignored.
 */
class CapsoolNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        appGraph.onNotificationListenerConnected(true)
    }

    override fun onListenerDisconnected() {
        appGraph.onNotificationListenerConnected(false)
        // Ask to be bound again; this is a no-op when the user has revoked access.
        requestRebind(ComponentName(this, CapsoolNotificationListener::class.java))
        super.onListenerDisconnected()
    }
}
