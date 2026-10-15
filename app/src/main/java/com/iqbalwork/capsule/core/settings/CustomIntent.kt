package com.iqbalwork.capsule.core.settings

enum class CustomIntentType { ACTIVITY, SERVICE, BROADCAST }

/**
 * User-defined custom intent action (F-76).
 *
 * [component] is formatted as "package/class" (e.g. "com.example/.MyReceiver" or
 * "com.example/com.example.MyReceiver"); null represents an implicit intent.
 * [icon] is the Material symbol icon name for UI display.
 */
data class CustomIntent(
    val id: String,
    val label: String,
    val type: CustomIntentType,
    val action: String,
    val component: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val icon: String = "bolt"
) {
    companion object {
        private const val RECORD = '\u001E'
        private const val FIELD = '\u001F'
        private const val EXTRA_ENTRY = '\u001D'
        private const val EXTRA_KV = '\u001C'

        /**
         * Encodes custom intents without org.json so the mapping stays testable on the JVM.
         * Extras entries are sorted by key for deterministic output.
         */
        fun encodeAll(intents: List<CustomIntent>): String = intents.joinToString(RECORD.toString()) { intent ->
            val encodedExtras = intent.extras.entries
                .sortedBy { it.key }
                .joinToString(EXTRA_ENTRY.toString()) { (k, v) -> "$k$EXTRA_KV$v" }

            listOf(
                intent.id,
                intent.label,
                intent.type.name,
                intent.action,
                intent.component.orEmpty(),
                encodedExtras,
                intent.icon
            ).joinToString(FIELD.toString())
        }

        fun decodeAll(value: String?): List<CustomIntent> = value.orEmpty().split(RECORD).mapNotNull { record ->
            if (record.isBlank()) return@mapNotNull null
            val fields = record.split(FIELD)
            if (fields.size < 6) return@mapNotNull null

            val id = fields[0]
            if (id.isEmpty()) return@mapNotNull null

            val label = fields[1]
            val type = runCatching { CustomIntentType.valueOf(fields[2]) }.getOrNull() ?: return@mapNotNull null
            val action = fields[3]
            if (action.isBlank()) return@mapNotNull null

            val component = fields[4].ifEmpty { null }
            val extras = if (fields[5].isEmpty()) {
                emptyMap()
            } else {
                val map = mutableMapOf<String, String>()
                fields[5].split(EXTRA_ENTRY).forEach { entry ->
                    val parts = entry.split(EXTRA_KV, limit = 2)
                    if (parts.size == 2 && parts[0].isNotEmpty()) {
                        map[parts[0]] = parts[1]
                    }
                }
                map
            }
            val icon = if (fields.size >= 7 && fields[6].isNotEmpty()) fields[6] else "bolt"

            CustomIntent(
                id = id,
                label = label,
                type = type,
                action = action,
                component = component,
                extras = extras,
                icon = icon
            )
        }
    }
}

/** Pure validator for custom intent input. */
object CustomIntentValidator {
    sealed interface Result {
        data object Valid : Result
        data class Error(val message: String) : Result
    }

    val Result.isValid: Boolean get() = this is Result.Valid

    fun validate(action: String, component: String?): Result {
        if (action.trim().isEmpty()) {
            return Result.Error("Action cannot be empty")
        }
        if (!component.isNullOrBlank()) {
            val trimmed = component.trim()
            val parts = trimmed.split('/')
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                return Result.Error("Component must follow package/class format (e.g. com.example/.MyReceiver)")
            }
        }
        return Result.Valid
    }
}
