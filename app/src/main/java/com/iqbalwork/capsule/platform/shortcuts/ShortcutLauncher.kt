package com.iqbalwork.capsule.platform.shortcuts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.SystemShortcut

private const val TAG = "CapsoolShortcuts"

/**
 * Opens shortcut targets (spec §2.5.7). Calls use the dialer (no CALL_PHONE permission);
 * WhatsApp uses its public wa.me link. Activities are started from the accessibility
 * service's process, which Android allows while the service is bound.
 */
class ShortcutLauncher(private val context: Context) {

    fun launch(shortcut: Shortcut) {
        val intent = when (shortcut.kind) {
            ShortcutKind.APP -> context.packageManager.getLaunchIntentForPackage(shortcut.target)
            ShortcutKind.CONTACT -> contactIntent(shortcut.target, shortcut.contactAction)
            ShortcutKind.SYSTEM -> runCatching { SystemShortcut.valueOf(shortcut.target) }.getOrNull()?.let(::systemIntent)
        }
        if (intent == null) {
            Log.w(TAG, "Nothing to open for ${shortcut.kind}")
            return
        }
        start(intent)
    }

    fun openApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let(::start)
            ?: Log.w(TAG, "App not installed or not launchable")
    }

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity to handle shortcut", e)
        } catch (e: SecurityException) {
            Log.w(TAG, "Shortcut refused", e)
        }
    }

    companion object {
        fun contactIntent(number: String, action: ContactAction): Intent = when (action) {
            ContactAction.CALL -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            ContactAction.WHATSAPP -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${whatsAppNumber(number)}"))
        }

        /** wa.me wants digits only, with the country code and no leading "+". */
        fun whatsAppNumber(number: String): String = number.filter { it.isDigit() }

        fun systemIntent(screen: SystemShortcut): Intent = Intent(
            when (screen) {
                SystemShortcut.INTERNET_PANEL -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                SystemShortcut.WIFI_PANEL -> Settings.Panel.ACTION_WIFI
                SystemShortcut.VOLUME_PANEL -> Settings.Panel.ACTION_VOLUME
                SystemShortcut.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
                SystemShortcut.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
                SystemShortcut.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                SystemShortcut.BATTERY -> Intent.ACTION_POWER_USAGE_SUMMARY
            }
        )
    }
}
