package com.iqbalwork.capsule.platform.notification

import android.app.Notification
import com.iqbalwork.capsule.R
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.model.CapsuleNotification
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.ProgressItem
import com.iqbalwork.capsule.model.TimerAction
import com.iqbalwork.capsule.model.TimerState
import com.iqbalwork.capsule.platform.PendingIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "CapsoolNotifications"
private const val AVATAR_PX = 128
private const val APP_ICON_PX = 48
private const val FALLBACK_GLOW = 0xFF8AB4F8L
private const val PROGRESS_THROTTLE_MS = 500L

/** Notification.EXTRA_SUBSTITUTE_APP_NAME is hidden API, but the key itself is stable. */
private const val EXTRA_SUBSTITUTE_APP_NAME = "android.substName"

/**
 * Turns posted notifications into [SystemEvent]s and executes notification effects
 * (spec §2.5.2). Notification text only lives in memory and is never logged.
 */
class NotificationSource(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: () -> Settings,
    private val knownKeys: () -> Set<String>,
    private val dispatch: (SystemEvent) -> Unit,
    ignoreOwnPackage: Boolean
) {
    private val filter = NotificationFilter(context.packageName, ignoreOwnPackage)
    private val posted = mutableMapOf<String, StatusBarNotification>()
    private val glowCache = ConcurrentHashMap<String, Long>() // filled from the parsing thread
    private var listener: NotificationListenerService? = null
    private val ownPackage = context.packageName
    private val ignoreOwnPackage = ignoreOwnPackage

    // Progress and timer bookkeeping (spec §2.5.6), main thread only.
    private val progressFraction = mutableMapOf<String, Float?>()
    private val progressDispatchedAt = mutableMapOf<String, Long>()
    private var timerKey: String? = null
    /** A paused timer notification no longer says it counts down; remember it per timer. */
    private var timerIsCountdown = false

    fun attach(service: NotificationListenerService) {
        listener = service
    }

    fun detach() {
        listener = null
        posted.clear()
    }

    fun onPosted(sbn: StatusBarNotification, ranking: NotificationListenerService.RankingMap?) {
        if (handleOngoing(sbn)) return
        val facts = factsOf(sbn, ranking)
        val decision = filter.decide(facts, settings().notificationBlockedApps, alreadyKnown = sbn.key in knownKeys())
        if (decision == NotificationDecision.IGNORE) return
        posted[sbn.key] = sbn
        scope.launch {
            val parsed = withContext(Dispatchers.Default) { parse(sbn) } ?: return@launch
            dispatch(SystemEvent.NotificationPosted(parsed, alert = decision == NotificationDecision.SHOW))
        }
    }

    fun onRemoved(sbn: StatusBarNotification) {
        posted.remove(sbn.key)
        if (sbn.key in progressFraction) finishProgress(sbn.key, sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
        if (sbn.key == timerKey) {
            timerKey = null
            dispatch(SystemEvent.TimerUpdated(null))
        }
        dispatch(SystemEvent.NotificationRemoved(sbn.key))
    }

    /** Routes progress and timer notifications to their cards. Returns true when it handled [sbn]. */
    private fun handleOngoing(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName in settings().notificationBlockedApps) return false
        if (ignoreOwnPackage && sbn.packageName == ownPackage) return false
        val facts = ongoingFactsOf(sbn)
        return when (OngoingClassifier.classify(facts, knownTimer = sbn.key == timerKey)) {
            OngoingKind.TIMER -> {
                posted[sbn.key] = sbn
                if (facts.showChronometer || sbn.key != timerKey) timerIsCountdown = facts.chronometerCountDown
                timerKey = sbn.key
                scope.launch { dispatch(SystemEvent.TimerUpdated(withContext(Dispatchers.Default) { parseTimer(sbn, facts) })) }
                true
            }
            OngoingKind.PROGRESS -> {
                if (OngoingClassifier.isFinished(facts) && !facts.isOngoing) {
                    if (sbn.key in progressFraction) finishProgress(sbn.key, titleOf(sbn), completed = true)
                    return false // a finished, dismissable download notification is an ordinary notification
                }
                val fraction = OngoingClassifier.fraction(facts)
                val previous = progressFraction[sbn.key]
                progressFraction[sbn.key] = fraction
                posted[sbn.key] = sbn
                // Downloads post several updates a second; the card does not need them all. New items,
                // completion and switches between determinate and indeterminate always go through.
                val now = SystemClock.elapsedRealtime()
                val last = progressDispatchedAt[sbn.key]
                val determinacyChanged = (previous == null) != (fraction == null)
                val urgent = last == null || fraction == 1f || determinacyChanged
                if (!urgent && now - last!! < PROGRESS_THROTTLE_MS) return true
                progressDispatchedAt[sbn.key] = now
                scope.launch { dispatch(SystemEvent.ProgressUpdated(withContext(Dispatchers.Default) { parseProgress(sbn, fraction) })) }
                true
            }
            OngoingKind.NONE -> {
                if (sbn.key in progressFraction) finishProgress(sbn.key, titleOf(sbn))
                if (sbn.key == timerKey) {
                    timerKey = null
                    dispatch(SystemEvent.TimerUpdated(null))
                }
                false
            }
        }
    }

    /** Ends a progress card; far enough along (or explicitly complete) it also says "done". */
    private fun finishProgress(key: String, title: String?, completed: Boolean = false) {
        val lastFraction = progressFraction.remove(key)
        progressDispatchedAt.remove(key)
        dispatch(SystemEvent.ProgressRemoved(key))
        if (completed || (lastFraction ?: 0f) >= OngoingClassifier.DONE_THRESHOLD) {
            dispatch(
                SystemEvent.MiniEventStarted(
                    MiniEvent(
                        id = System.nanoTime(),
                        type = MiniEventType.PROGRESS_DONE,
                        title = context.getString(R.string.event_done),
                        subtitle = title.orEmpty()
                    )
                )
            )
        }
    }

    private fun titleOf(sbn: StatusBarNotification): String? =
        sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

    private fun ongoingFactsOf(sbn: StatusBarNotification): OngoingFacts {
        val notification = sbn.notification
        val extras = notification.extras
        return OngoingFacts(
            packageName = sbn.packageName,
            isOngoing = sbn.isOngoing,
            isMediaStyle = extras.getString(Notification.EXTRA_TEMPLATE) == Notification.MediaStyle::class.java.name,
            category = notification.category,
            progress = extras.getInt(Notification.EXTRA_PROGRESS, 0),
            progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false),
            showChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false),
            chronometerCountDown = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false)
        )
    }

    private fun parseTimer(sbn: StatusBarNotification, facts: OngoingFacts): TimerState {
        val notification = sbn.notification
        val extras = notification.extras
        return TimerState(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName(sbn),
            label = titleOf(sbn) ?: appName(sbn),
            isCountdown = timerIsCountdown,
            baseTimeMs = notification.`when`,
            isRunning = facts.showChronometer,
            staticText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            actions = notification.actions.orEmpty().mapIndexedNotNull { index, action ->
                action.title?.toString()?.takeIf { it.isNotBlank() }?.let { TimerAction(index, it) }
            },
            appIcon = notification.smallIcon?.loadDrawable(context)?.toBitmap(APP_ICON_PX, APP_ICON_PX)
        )
    }

    private fun parseProgress(sbn: StatusBarNotification, fraction: Float?): ProgressItem = ProgressItem(
        key = sbn.key,
        packageName = sbn.packageName,
        appName = appName(sbn),
        title = titleOf(sbn) ?: appName(sbn),
        fraction = fraction,
        appIcon = sbn.notification.smallIcon?.loadDrawable(context)?.toBitmap(APP_ICON_PX, APP_ICON_PX),
        color = glowColor(sbn)
    )

    fun execute(effect: Effect) {
        when (effect) {
            is Effect.NotificationReply -> reply(effect.key, effect.text)
            is Effect.NotificationMarkRead -> posted[effect.key]?.notification?.actions
                ?.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ }
                ?.actionIntent?.let(::sendQuietly)
            is Effect.NotificationDismiss -> runCatching { listener?.cancelNotification(effect.key) }
                .onFailure { Log.w(TAG, "Dismiss failed", it) }
            is Effect.NotificationOpen -> posted[effect.key]?.notification?.contentIntent?.let(::sendLaunching)
            is Effect.NotificationAction -> posted[effect.key]?.notification?.actions
                ?.getOrNull(effect.index)?.actionIntent?.let(::sendQuietly)
            else -> Unit
        }
    }

    private fun reply(key: String, text: String) {
        val action = posted[key]?.notification?.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return
        val remoteInputs = action.remoteInputs ?: return
        val results = Bundle().apply {
            remoteInputs.filter { it.allowFreeFormInput }.forEach { putCharSequence(it.resultKey, text) }
        }
        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        RemoteInput.addResultsToIntent(remoteInputs, intent, results)
        runCatching { action.actionIntent.send(context, 0, intent) }
            .onFailure { Log.w(TAG, "Reply failed", it) }
    }

    private fun sendQuietly(intent: PendingIntent) {
        runCatching { intent.send() }.onFailure { Log.w(TAG, "Notification action failed", it) }
    }

    private fun sendLaunching(intent: PendingIntent) {
        runCatching { PendingIntents.sendLaunching(context, intent) }
            .onFailure { Log.w(TAG, "Opening notification failed", it) }
    }

    private fun factsOf(sbn: StatusBarNotification, ranking: NotificationListenerService.RankingMap?): NotificationFacts {
        val notification = sbn.notification
        val extras = notification.extras
        val importance = NotificationListenerService.Ranking()
            .takeIf { ranking?.getRanking(sbn.key, it) == true }
            ?.importance ?: android.app.NotificationManager.IMPORTANCE_DEFAULT
        val hasContent = !extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank() ||
            !extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank()
        return NotificationFacts(
            key = sbn.key,
            packageName = sbn.packageName,
            isOngoing = sbn.isOngoing,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isMediaStyle = extras.getString(Notification.EXTRA_TEMPLATE) == Notification.MediaStyle::class.java.name,
            category = notification.category,
            importance = importance,
            hasContent = hasContent,
            onlyAlertOnce = notification.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0
        )
    }

    private fun parse(sbn: StatusBarNotification): CapsuleNotification? {
        val notification = sbn.notification
        val extras = notification.extras
        val messaging = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val lastMessage = messaging?.messages?.lastOrNull()
        val sender = lastMessage?.person?.name?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: return null
        val text = lastMessage?.text?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: ""
        val avatar = lastMessage?.person?.icon?.loadDrawable(context)?.toBitmap(AVATAR_PX, AVATAR_PX)
            ?: notification.getLargeIcon()?.loadDrawable(context)?.toBitmap(AVATAR_PX, AVATAR_PX)
        val appIcon = notification.smallIcon?.loadDrawable(context)?.toBitmap(APP_ICON_PX, APP_ICON_PX)
        val actions = notification.actions.orEmpty()

        return CapsuleNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName(sbn),
            sender = sender,
            text = text,
            conversationTitle = messaging?.conversationTitle?.toString()?.takeIf { messaging.isGroupConversation },
            avatar = avatar,
            appIcon = appIcon,
            glowColor = glowColor(sbn),
            postedAt = sbn.postTime,
            canReply = actions.any { action -> action.remoteInputs?.any { it.allowFreeFormInput } == true },
            canMarkRead = actions.any { it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ },
            clearable = sbn.isClearable,
            isPublic = notification.visibility == Notification.VISIBILITY_PUBLIC
        )
    }

    private fun appName(sbn: StatusBarNotification): String =
        sbn.notification.extras.getString(EXTRA_SUBSTITUTE_APP_NAME)
            ?: runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            }.getOrDefault(sbn.packageName)

    /** The app's accent: its notification color, else the launcher icon's vibrant color. */
    private fun glowColor(sbn: StatusBarNotification): Long {
        val declared = sbn.notification.color
        if (declared != Notification.COLOR_DEFAULT && declared != 0) return declared.toLong() and 0xFFFFFFFFL
        return glowCache.getOrPut(sbn.packageName) {
            runCatching {
                val icon: Bitmap = context.packageManager.getApplicationIcon(sbn.packageName).toBitmap(64, 64)
                val palette = Palette.from(icon).generate()
                (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.toLong()?.and(0xFFFFFFFFL)
            }.getOrNull() ?: FALLBACK_GLOW
        }
    }
}
