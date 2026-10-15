package com.iqbalwork.capsule.platform

import android.content.Context
import android.content.Intent

/**
 * F-73: the broadcast automation apps (Tasker, MacroDroid, Automate) can listen for.
 * Extras: type = "gesture", gesture = tap|double_tap|long_press|swipe_left|swipe_right,
 * action = what Capsool did, card = the card showing (if any).
 */
object Automation {
    const val ACTION_EVENT = "com.iqbalwork.capsule.action.EVENT"
    const val EXTRA_TYPE = "type"
    const val EXTRA_GESTURE = "gesture"
    const val EXTRA_ACTION = "action"
    const val EXTRA_CARD = "card"

    fun sendGesture(context: Context, gesture: String, action: String, card: String?) {
        context.sendBroadcast(
            Intent(ACTION_EVENT)
                .putExtra(EXTRA_TYPE, "gesture")
                .putExtra(EXTRA_GESTURE, gesture)
                .putExtra(EXTRA_ACTION, action)
                .putExtra(EXTRA_CARD, card)
        )
    }
}
