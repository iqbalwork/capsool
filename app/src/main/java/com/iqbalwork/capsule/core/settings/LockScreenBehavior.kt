package com.iqbalwork.capsule.core.settings

/**
 * F-85: what the capsule does while the device is locked.
 *
 * [SHOW_WITHOUT_CONTENT] is the default because notification bodies are private things to put on
 * a locked screen; the capsule itself is still useful there (media controls, charging).
 */
enum class LockScreenBehavior(val displayName: String) {
    SHOW("Show"),
    SHOW_WITHOUT_CONTENT("Show without content"),
    HIDE("Hide")
}
