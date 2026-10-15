package com.iqbalwork.capsule.core.settings

enum class StaticCardType(val label: String) {
    SHORTCUTS("Shortcuts"),
    BARCODE("Barcode"),
    SLIDERS("Controls"),
    INTENT("Custom action")
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
 * Salin kartu turunan (effectiveStaticCards) ke staticCards eksplisit agar user bisa mulai
 * mengatur dari keadaan sekarang tanpa mengulang dari nol. No-op jika staticCards sudah terisi.
 */
fun Settings.seedStaticCards(): Settings =
    if (staticCards.isNotEmpty()) this
    else copy(staticCards = effectiveStaticCards)
