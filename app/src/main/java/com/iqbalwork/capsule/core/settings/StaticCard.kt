package com.iqbalwork.capsule.core.settings

import androidx.annotation.StringRes
import com.iqbalwork.capsule.R

/**
 * [label] is the stable default name stored with a card and compared against to tell "the user
 * renamed it" from "still the default"; it is never shown. Show [labelRes] instead.
 */
enum class StaticCardType(val label: String, @StringRes val labelRes: Int) {
    SHORTCUTS("Shortcuts", R.string.static_type_shortcuts),
    BARCODE("Barcode", R.string.static_type_barcode),
    SLIDERS("Controls", R.string.static_type_sliders),
    INTENT("Custom action", R.string.static_type_intent)
}

enum class StaticCardViewMode { CAROUSEL, LIST }

data class StaticCard(
    val id: String,
    val type: StaticCardType,
    val label: String
) {
    companion object {
        private const val RECORD = '\u001E'
        private const val FIELD = '\u001F'

        fun encodeAll(cards: List<StaticCard>): String = cards.joinToString(RECORD.toString()) { card ->
            val sanitizedLabel = card.label.replace(FIELD.toString(), "").replace(RECORD.toString(), "")
            listOf(card.id, card.type.name, sanitizedLabel).joinToString(FIELD.toString())
        }

        fun decodeAll(value: String?): List<StaticCard> = value.orEmpty().split(RECORD).mapNotNull { record ->
            if (record.isBlank()) return@mapNotNull null
            val fields = record.split(FIELD)
            if (fields.size < 3) return@mapNotNull null
            val id = fields[0]
            if (id.isEmpty()) return@mapNotNull null
            val type = runCatching { StaticCardType.valueOf(fields[1]) }.getOrNull() ?: return@mapNotNull null
            val label = fields[2]
            StaticCard(id, type, label)
        }
    }
}

fun List<StaticCard>.moveUp(id: String): List<StaticCard> {
    val index = indexOfFirst { it.id == id }
    if (index <= 0) return this
    val mutable = toMutableList()
    val temp = mutable[index]
    mutable[index] = mutable[index - 1]
    mutable[index - 1] = temp
    return mutable
}

fun List<StaticCard>.moveDown(id: String): List<StaticCard> {
    val index = indexOfFirst { it.id == id }
    if (index < 0 || index >= size - 1) return this
    val mutable = toMutableList()
    val temp = mutable[index]
    mutable[index] = mutable[index + 1]
    mutable[index + 1] = temp
    return mutable
}

fun List<StaticCard>.without(id: String): List<StaticCard> = filter { it.id != id }

/**
 * Copies the derived cards into an explicit list so the user can start arranging from the
 * current state instead of from scratch. No-op once the list is explicit.
 */
fun Settings.seedStaticCards(): Settings =
    if (staticCards.isNotEmpty()) this
    else copy(staticCards = effectiveStaticCards)
