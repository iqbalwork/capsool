package com.iqbalwork.capsule.platform

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.platform.media.MediaSource

private const val TAG = "CapsoolEffects"

/**
 * Executes reducer effects against the system. Effects for features that are not wired
 * yet are logged by type only (never with their payload, which may hold message text).
 */
class PlatformEffectHandler(private val mediaSource: MediaSource) {

    /** Set while the accessibility service is connected; global actions need it. */
    @Volatile
    var accessibilityService: AccessibilityService? = null

    fun handle(effect: Effect) {
        when (effect) {
            is Effect.Global -> performGlobal(effect.action)
            is Effect.Media, is Effect.MediaSeek, Effect.MediaOpenApp -> mediaSource.execute(effect)
            // Wired in later milestones: notifications (M2), sliders & torch (M4), shortcuts (M6).
            else -> Log.d(TAG, "Effect not wired yet: ${effect::class.simpleName}")
        }
    }

    private fun performGlobal(action: Effect.GlobalAction) {
        val service = accessibilityService ?: return
        val global = when (action) {
            Effect.GlobalAction.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
            Effect.GlobalAction.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
        }
        service.performGlobalAction(global)
    }
}
