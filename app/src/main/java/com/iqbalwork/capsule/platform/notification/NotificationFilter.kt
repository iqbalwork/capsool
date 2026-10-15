package com.iqbalwork.capsule.platform.notification

import android.app.Notification
import android.app.NotificationManager

/** The facts about a posted notification that decide whether the capsule shows it. */
data class NotificationFacts(
    val key: String,
    val packageName: String,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val isMediaStyle: Boolean,
    val category: String?,
    val importance: Int,
    val hasContent: Boolean,
    val onlyAlertOnce: Boolean
)

enum class NotificationDecision {
    /** New or alerting again: put it at the front of the capsule. */
    SHOW,
    /** An update that should not pop up again: refresh it only if the capsule already has it. */
    UPDATE_IN_PLACE,
    IGNORE
}

/**
 * Which notifications reach the capsule (spec §2.5.2). Pure so it can be unit-tested; the
 * Android side only extracts [NotificationFacts].
 */
class NotificationFilter(private val ownPackage: String, private val ignoreOwnPackage: Boolean = true) {

    fun decide(facts: NotificationFacts, blockedApps: Set<String>, alreadyKnown: Boolean): NotificationDecision {
        val ignored = (ignoreOwnPackage && facts.packageName == ownPackage) ||
            // System UI's own notices (battery, USB debugging, screenshots) are not messages;
            // battery has its own mini events.
            facts.packageName in SystemPackages ||
            facts.packageName in blockedApps ||
            facts.isGroupSummary ||
            // Ongoing notifications (downloads, timers, calls) are M5 cards, media has its own card.
            facts.isOngoing ||
            facts.isMediaStyle ||
            facts.category in IgnoredCategories ||
            facts.importance < NotificationManager.IMPORTANCE_DEFAULT ||
            !facts.hasContent
        return when {
            ignored -> NotificationDecision.IGNORE
            alreadyKnown && facts.onlyAlertOnce -> NotificationDecision.UPDATE_IN_PLACE
            else -> NotificationDecision.SHOW
        }
    }

    private companion object {
        val SystemPackages = setOf("android", "com.android.systemui")
        val IgnoredCategories = setOf(
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_STATUS
        )
    }
}
