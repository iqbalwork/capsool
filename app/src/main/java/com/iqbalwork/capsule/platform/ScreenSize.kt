package com.iqbalwork.capsule.platform

import android.content.Context
import android.view.WindowManager

/** Pixel size of the whole display (not the app window), so it changes only with the panel or resolution. */
object ScreenSize {
    fun of(context: Context): Pair<Int, Int> {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return bounds.width() to bounds.height()
    }
}
