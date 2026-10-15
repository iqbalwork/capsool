package com.iqbalwork.capsule.ui.companion

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Process
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(val packageName: String, val label: String, val icon: Bitmap)

/** Launchable apps via LauncherApps, which needs no package-visibility permission. */
object LaunchableApps {
    fun load(context: Context): List<LaunchableApp> {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val iconPx = (36 * context.resources.displayMetrics.density).toInt()
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .distinctBy { it.applicationInfo.packageName }
            .filter { it.applicationInfo.packageName != context.packageName }
            .map {
                LaunchableApp(
                    packageName = it.applicationInfo.packageName,
                    label = it.label.toString(),
                    icon = it.getIcon(0).toBitmap(iconPx, iconPx)
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
