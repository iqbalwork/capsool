package com.iqbalwork.capsule.platform

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.security.advancedprotection.AdvancedProtectionManager
import com.iqbalwork.capsule.platform.notification.CapsoolNotificationListener

/** Read-only checks and settings shortcuts used by the onboarding and status screens. */
object SystemStatus {

    /**
     * Android Advanced Protection (Android 16+) may switch off accessibility services that are
     * not accessibility tools, which stops the capsule (F-97). There is no workaround; we only tell the user.
     */
    fun isAdvancedProtectionEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false
        return runCatching {
            context.getSystemService(AdvancedProtectionManager::class.java)?.isAdvancedProtectionEnabled == true
        }.getOrDefault(false)
    }

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens notification access for Capsool's listener; some OEMs only have the full list. */
    fun openNotificationAccess(context: Context) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, CapsoolNotificationListener::class.java).flattenToString()
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(detail)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** App info screen, where sideloaded apps unlock "restricted settings" on Android 13+. */
    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
