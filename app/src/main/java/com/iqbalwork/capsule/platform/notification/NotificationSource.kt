package com.iqbalwork.capsule.platform.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
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

    fun attach(service: NotificationListenerService) {
        listener = service
    }

    fun detach() {
        listener = null
        posted.clear()
    }

    fun onPosted(sbn: StatusBarNotification, ranking: NotificationListenerService.RankingMap?) {
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
        dispatch(SystemEvent.NotificationRemoved(sbn.key))
    }

    fun execute(effect: Effect) {
        when (effect) {
            is Effect.NotificationReply -> reply(effect.key, effect.text)
            is Effect.NotificationMarkRead -> posted[effect.key]?.notification?.actions
                ?.firstOrNull { it.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ }
                ?.actionIntent?.let(::sendQuietly)
            is Effect.NotificationDismiss -> runCatching { listener?.cancelNotification(effect.key) }
                .onFailure { Log.w(TAG, "Dismiss failed", it) }
            is Effect.NotificationOpen -> posted[effect.key]?.notification?.contentIntent?.let(::sendLaunching)
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
