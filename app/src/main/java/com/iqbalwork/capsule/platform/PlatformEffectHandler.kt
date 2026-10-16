package com.iqbalwork.capsule.platform

import android.accessibilityservice.AccessibilityService
import com.iqbalwork.capsule.platform.intents.IntentResult
import com.iqbalwork.capsule.model.MiniEventType
import com.iqbalwork.capsule.model.MiniEvent
import com.iqbalwork.capsule.R
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.store.SystemEvent
import com.iqbalwork.capsule.platform.accessibility.ScrollToTop
import com.iqbalwork.capsule.platform.shortcuts.ShortcutLauncher
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.platform.controls.ControlsSource
import com.iqbalwork.capsule.platform.media.MediaSource
import com.iqbalwork.capsule.platform.notification.NotificationSource

import com.iqbalwork.capsule.platform.intents.CustomIntentRunner

/** Executes reducer effects against the system. Effect payloads (message text) are never logged. */
class PlatformEffectHandler(
    private val context: Context,
    private val mediaSource: MediaSource,
    private val notificationSource: NotificationSource,
    private val controls: ControlsSource,
    private val settings: () -> Settings,
    private val dispatch: (SystemEvent) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val launcher = ShortcutLauncher(context)
    private val customIntentRunner = CustomIntentRunner(context)

    /** Set while the accessibility service is connected; global actions need it. */
    @Volatile
    var accessibilityService: AccessibilityService? = null

    fun handle(effect: Effect) {
        when (effect) {
            is Effect.Global -> performGlobal(effect.action)
            is Effect.Media, is Effect.MediaSeek, Effect.MediaOpenApp -> mediaSource.execute(effect)
            is Effect.NotificationReply, is Effect.NotificationMarkRead,
            is Effect.NotificationDismiss, is Effect.NotificationOpen,
            is Effect.NotificationAction -> notificationSource.execute(effect)
            is Effect.Volume, is Effect.Ringer, is Effect.Brightness, is Effect.AdaptiveBrightness,
            is Effect.Torch, is Effect.OpenPermission, Effect.RefreshControls -> controls.execute(effect)
            is Effect.LaunchShortcut -> settings().shortcuts.firstOrNull { it.id == effect.id }?.let(launcher::launch)
            // Fired from a gesture: a failure is shown as a short "Action failed" pop-up naming the
            // intent. The reason itself is long, so the "Send test" button in the companion app
            // (IntentsTab) remains where it is explained.
            is Effect.SendCustomIntent -> settings().customIntents.firstOrNull { it.id == effect.id }?.let { intent ->
                if (customIntentRunner.run(intent) is IntentResult.Error) {
                    dispatch(
                        SystemEvent.MiniEventStarted(
                            MiniEvent(
                                id = System.nanoTime(),
                                type = MiniEventType.ACTION_FAILED,
                                title = context.getString(R.string.event_action_failed),
                                subtitle = intent.label
                            )
                        )
                    )
                }
            }
            is Effect.OpenApp -> launcher.openApp(effect.packageName)
            Effect.ScrollToTop -> accessibilityService?.let(ScrollToTop::run)
            is Effect.AutomationBroadcast -> Automation.sendGesture(context, effect.gesture, effect.action, effect.card)
        }
    }

    private fun performGlobal(action: Effect.GlobalAction) {
        val service = accessibilityService
        if (action == Effect.GlobalAction.SCREENSHOT) {
            // The reducer already hid the capsule; give the overlay a moment to disappear first.
            handler.postDelayed({
                service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                handler.postDelayed({ dispatch(SystemEvent.ScreenshotDone) }, SCREENSHOT_SETTLE_MS)
            }, SCREENSHOT_HIDE_MS)
            return
        }
        service ?: return
        val global = when (action) {
            Effect.GlobalAction.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            Effect.GlobalAction.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            Effect.GlobalAction.LOCK_SCREEN -> AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            Effect.GlobalAction.SCREENSHOT -> return
        }
        service.performGlobalAction(global)
    }

    private companion object {
        const val SCREENSHOT_HIDE_MS = 200L
        const val SCREENSHOT_SETTLE_MS = 800L
    }
}
