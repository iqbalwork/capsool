package com.iqbalwork.capsule.core.settings

import androidx.annotation.StringRes
import com.iqbalwork.capsule.R

enum class ShortcutKind { APP, CONTACT, SYSTEM }

enum class ContactAction { CALL, WHATSAPP }

/** System screens a shortcut can open (F-56); panels slide up over the current app. */
enum class SystemShortcut(@StringRes val labelRes: Int) {
    INTERNET_PANEL(R.string.sys_internet),
    WIFI_PANEL(R.string.sys_wifi),
    VOLUME_PANEL(R.string.sys_volume),
    BLUETOOTH(R.string.sys_bluetooth),
    DISPLAY(R.string.sys_display),
    LOCATION(R.string.sys_location),
    BATTERY(R.string.sys_battery)
}

/**
 * One tile on the Shortcuts card (F-54..F-56). [target] is a package for APP, a phone
 * number for CONTACT and a [SystemShortcut] name for SYSTEM.
 */
data class Shortcut(
    val id: String,
    val kind: ShortcutKind,
    val label: String,
    val target: String,
    val contactAction: ContactAction = ContactAction.CALL
) {
    companion object {
        // Control characters never appear in labels, numbers or package names.
        private const val FIELD = '\u001F'
        private const val RECORD = '\u001E'

        /** Stored without org.json so the mapping stays testable on the JVM. */
        fun encodeAll(shortcuts: List<Shortcut>): String = shortcuts.joinToString(RECORD.toString()) {
            listOf(it.id, it.kind.name, it.label, it.target, it.contactAction.name).joinToString(FIELD.toString())
        }

        fun decodeAll(value: String?): List<Shortcut> = value.orEmpty().split(RECORD).mapNotNull { record ->
            val fields = record.split(FIELD)
            if (fields.size < 5) return@mapNotNull null
            val kind = runCatching { ShortcutKind.valueOf(fields[1]) }.getOrNull() ?: return@mapNotNull null
            val action = runCatching { ContactAction.valueOf(fields[4]) }.getOrDefault(ContactAction.CALL)
            Shortcut(fields[0], kind, fields[2], fields[3], action)
        }
    }
}
