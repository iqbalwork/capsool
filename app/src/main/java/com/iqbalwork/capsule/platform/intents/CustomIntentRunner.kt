package com.iqbalwork.capsule.platform.intents

import android.content.ActivityNotFoundException
import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.R
import android.content.Context
import android.content.Intent
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType

sealed interface IntentResult {
    data object Ok : IntentResult
    data class Error(val message: UiText) : IntentResult
}

/**
 * Executes a [CustomIntent] against Android's platform APIs (F-76).
 *
 * Real system constraints:
 * - On Android 12+, components that are not exported (`android:exported="false"`) cannot be triggered
 *   from another app without appropriate signature/permissions, producing a [SecurityException].
 * - On Android 14+, background activity and foreground service starts are heavily restricted
 *   unless specific exemptions apply, producing [IllegalStateException] or [SecurityException].
 */
class CustomIntentRunner(private val context: Context) {

    fun run(customIntent: CustomIntent): IntentResult {
        if (customIntent.action.isBlank() && customIntent.component.isNullOrBlank()) {
            return IntentResult.Error(UiText.res(R.string.intent_run_action_empty))
        }

        return try {
            val intent = buildIntent(customIntent)
            when (customIntent.type) {
                CustomIntentType.ACTIVITY -> {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    IntentResult.Ok
                }
                CustomIntentType.SERVICE -> {
                    context.startForegroundService(intent)
                    IntentResult.Ok
                }
                CustomIntentType.BROADCAST -> {
                    context.sendBroadcast(intent)
                    IntentResult.Ok
                }
            }
        } catch (e: ActivityNotFoundException) {
            val target = customIntent.component ?: customIntent.action
            IntentResult.Error(UiText.res(R.string.intent_run_not_found, target))
        } catch (e: SecurityException) {
            IntentResult.Error(UiText.res(R.string.intent_run_security, detail(e)))
        } catch (e: IllegalStateException) {
            IntentResult.Error(UiText.res(R.string.intent_run_illegal_state, detail(e)))
        } catch (e: IllegalArgumentException) {
            IntentResult.Error(UiText.res(R.string.intent_run_bad_argument, detail(e)))
        } catch (e: ClassNotFoundException) {
            IntentResult.Error(UiText.res(R.string.intent_run_class_not_found, detail(e)))
        } catch (e: Exception) {
            IntentResult.Error(UiText.res(R.string.intent_run_unknown, e.javaClass.simpleName, detail(e)))
        }
    }

    companion object {
        fun buildIntent(customIntent: CustomIntent): Intent {
            val intent = Intent()
            if (customIntent.action.isNotBlank()) {
                intent.action = customIntent.action
            }
            customIntent.extras.forEach { (k, v) ->
                intent.putExtra(k, v)
            }
            val comp = customIntent.component?.trim()
            if (!comp.isNullOrEmpty()) {
                val parts = comp.split('/', limit = 2)
                if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    val pkg = parts[0]
                    val cls = if (parts[1].startsWith('.')) pkg + parts[1] else parts[1]
                    intent.setClassName(pkg, cls)
                }
            }
            return intent
        }
    }

    /** The system's own wording for [e]; it is not ours to translate, so it is passed in as an argument. */
    private fun detail(e: Exception): String =
        e.localizedMessage ?: e.message ?: e.javaClass.simpleName
}
