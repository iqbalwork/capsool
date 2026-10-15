package com.iqbalwork.capsule.engine.stack

import com.iqbalwork.capsule.model.CapsuleCardType

/**
 * Manages the carousel stack of active dynamic cards.
 * Provides continuous next/previous card cycling on horizontal swipes.
 */
class CardStackManager(
    initialCards: List<CapsuleCardType> = listOf(
        CapsuleCardType.MEDIA,
        CapsuleCardType.SLIDERS,
        CapsuleCardType.CHARGING,
        CapsuleCardType.NOTIFICATION,
        CapsuleCardType.TIMER,
        CapsuleCardType.SHORTCUTS
    )
) {
    private val activeCards: MutableList<CapsuleCardType> = initialCards.toMutableList().also {
        if (it.isEmpty()) it.add(CapsuleCardType.MEDIA)
    }

    private var currentIndex: Int = 0

    @Synchronized
    fun getCards(): List<CapsuleCardType> = activeCards.toList()

    @Synchronized
    fun getActiveCard(): CapsuleCardType {
        if (currentIndex !in activeCards.indices) {
            currentIndex = 0
        }
        return activeCards[currentIndex]
    }

    @Synchronized
    fun getActiveIndex(): Int = currentIndex

    @Synchronized
    fun nextCard(): CapsuleCardType {
        if (activeCards.isEmpty()) return CapsuleCardType.MEDIA
        currentIndex = (currentIndex + 1) % activeCards.size
        return activeCards[currentIndex]
    }

    @Synchronized
    fun previousCard(): CapsuleCardType {
        if (activeCards.isEmpty()) return CapsuleCardType.MEDIA
        currentIndex = if (currentIndex - 1 < 0) activeCards.size - 1 else currentIndex - 1
        return activeCards[currentIndex]
    }

    @Synchronized
    fun setActiveCard(cardType: CapsuleCardType): Boolean {
        val index = activeCards.indexOf(cardType)
        return if (index != -1) {
            currentIndex = index
            true
        } else {
            false
        }
    }

    @Synchronized
    fun addCard(cardType: CapsuleCardType) {
        if (!activeCards.contains(cardType)) {
            activeCards.add(cardType)
        }
    }

    @Synchronized
    fun removeCard(cardType: CapsuleCardType) {
        if (activeCards.size > 1) {
            val removingIndex = activeCards.indexOf(cardType)
            if (removingIndex != -1) {
                activeCards.removeAt(removingIndex)
                if (currentIndex >= activeCards.size) {
                    currentIndex = 0
                }
            }
        }
    }

    @Synchronized
    fun setCardEnabled(cardType: CapsuleCardType, enabled: Boolean) {
        if (enabled) {
            addCard(cardType)
        } else {
            removeCard(cardType)
        }
    }

    @Synchronized
    fun isCardEnabled(cardType: CapsuleCardType): Boolean = activeCards.contains(cardType)
}
