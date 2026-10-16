package com.iqbalwork.capsule.platform.intents

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType

sealed interface IntentResult {
    data object Ok : IntentResult
    data class Error(val message: String) : IntentResult
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
            return IntentResult.Error("Action cannot be empty.")
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
            IntentResult.Error("Activity not found for '$target'. Check that the target app is installed and the action/component is correct.")
        } catch (e: SecurityException) {
            IntentResult.Error("Security error: ${e.localizedMessage ?: e.message ?: "Permission denied"}. On Android 12+, components must be exported (android:exported=\"true\") or require specific permissions to be triggered from another app.")
        } catch (e: IllegalStateException) {
            IntentResult.Error("Illegal state error: ${e.localizedMessage ?: e.message ?: "Action not allowed"}. Android restricts background service and activity launches (especially on Android 14+). Ensure background launch is permitted.")
        } catch (e: IllegalArgumentException) {
            IntentResult.Error("Invalid intent argument: ${e.localizedMessage ?: e.message ?: "Invalid parameter"}.")
        } catch (e: ClassNotFoundException) {
            IntentResult.Error("Component class not found: ${e.localizedMessage ?: e.message ?: "Class not found"}. Verify the component package and class name.")
        } catch (e: Exception) {
            IntentResult.Error("Execution error (${e.javaClass.simpleName}): ${e.localizedMessage ?: e.message ?: "Unknown error"}")
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
}
