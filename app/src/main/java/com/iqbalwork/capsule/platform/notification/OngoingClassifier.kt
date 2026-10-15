package com.iqbalwork.capsule.platform.notification

import android.app.Notification

/** The parts of a notification that decide whether it is a progress bar or a live timer. */
data class OngoingFacts(
    val packageName: String,
    val isOngoing: Boolean,
    val isMediaStyle: Boolean,
    val category: String?,
    val progress: Int,
    val progressMax: Int,
    val indeterminate: Boolean,
    val showChronometer: Boolean,
    val chronometerCountDown: Boolean
)

enum class OngoingKind { PROGRESS, TIMER, NONE }

/**
 * Sorts notifications into progress and timer cards using only standard notification fields,
 * so it works for any app that uses them, not a hardcoded list (spec §2.5.6). Pure.
 */
object OngoingClassifier {

    fun classify(facts: OngoingFacts, knownTimer: Boolean): OngoingKind {
        // Calls and navigation also show a chronometer or progress; they are not timers or downloads.
        if (facts.isMediaStyle || facts.category in Excluded) return OngoingKind.NONE
        return when {
            facts.isOngoing && facts.showChronometer -> OngoingKind.TIMER
            // A paused clock timer drops the chronometer but stays ongoing.
            facts.isOngoing && knownTimer -> OngoingKind.TIMER
            facts.indeterminate || facts.progressMax > 0 -> OngoingKind.PROGRESS
            else -> OngoingKind.NONE
        }
    }

    fun fraction(facts: OngoingFacts): Float? =
        if (facts.indeterminate || facts.progressMax <= 0) null
        else (facts.progress.toFloat() / facts.progressMax).coerceIn(0f, 1f)

    fun isFinished(facts: OngoingFacts): Boolean =
        !facts.indeterminate && facts.progressMax > 0 && facts.progress >= facts.progressMax

    /** A progress notification that disappears this far along counts as done, not cancelled. */
    const val DONE_THRESHOLD = 0.9f

    private val Excluded = setOf(Notification.CATEGORY_CALL, Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_TRANSPORT)
}
