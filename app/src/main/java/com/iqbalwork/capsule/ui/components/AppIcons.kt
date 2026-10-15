package com.iqbalwork.capsule.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app icon: either full color, or the monochrome layer to tint (themed icons, F-54). */
data class AppIcon(val bitmap: Bitmap, val monochrome: Boolean)

private const val ICON_PX = 96
private val iconCache = LruCache<String, AppIcon>(48)

/**
 * Loads (off the main thread) and caches an app's icon. With [themed] on Android 13+, uses the
 * adaptive icon's monochrome layer when the app has one, like themed launcher icons.
 */
@Composable
fun rememberAppIcon(packageName: String, themed: Boolean): State<AppIcon?> {
    val context = LocalContext.current
    val key = "$packageName:$themed"
    return produceState(iconCache.get(key), key) {
        if (value == null) value = withContext(Dispatchers.IO) { loadIcon(context, packageName, themed) }?.also { iconCache.put(key, it) }
    }
}

private fun loadIcon(context: Context, packageName: String, themed: Boolean): AppIcon? = runCatching {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    val monochrome = if (themed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        (drawable as? AdaptiveIconDrawable)?.monochrome
    } else {
        null
    }
    if (monochrome != null) AppIcon(monochrome.toBitmap(ICON_PX, ICON_PX), monochrome = true)
    else AppIcon(drawable.toBitmap(ICON_PX, ICON_PX), monochrome = false)
}.getOrNull()
