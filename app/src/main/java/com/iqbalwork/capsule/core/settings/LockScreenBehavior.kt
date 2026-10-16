package com.iqbalwork.capsule.core.settings

import androidx.annotation.StringRes
import com.iqbalwork.capsule.R

/**
 * F-85: what the capsule does while the device is locked.
 *
 * [SHOW_WITHOUT_CONTENT] is the default because notification bodies are private things to put on
 * a locked screen; the capsule itself is still useful there (media controls, charging).
 */
enum class LockScreenBehavior(@StringRes val labelRes: Int) {
    SHOW(R.string.lock_show),
    SHOW_WITHOUT_CONTENT(R.string.lock_show_without_content),
    HIDE(R.string.lock_hide)
}
