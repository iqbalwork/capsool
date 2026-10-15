package com.iqbalwork.capsule

import com.iqbalwork.capsule.engine.stack.CardStackManager
import com.iqbalwork.capsule.model.CapsuleCardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardStackManagerTest {

    private lateinit var manager: CardStackManager

    @Before
    fun setUp() {
        manager = CardStackManager()
    }

    @Test
    fun testInitialCardsContainFullParityStack() {
        val cards = manager.getCards()
        assertEquals(6, cards.size)
        assertTrue(cards.contains(CapsuleCardType.MEDIA))
        assertTrue(cards.contains(CapsuleCardType.SLIDERS))
        assertTrue(cards.contains(CapsuleCardType.CHARGING))
        assertTrue(cards.contains(CapsuleCardType.NOTIFICATION))
        assertTrue(cards.contains(CapsuleCardType.TIMER))
        assertTrue(cards.contains(CapsuleCardType.SHORTCUTS))
        assertEquals(CapsuleCardType.MEDIA, manager.getActiveCard())
    }

    @Test
    fun testNextCardCarouselWrapping() {
        val cards = manager.getCards()
        for (i in 1 until cards.size) {
            val next = manager.nextCard()
            assertEquals(cards[i], next)
        }
        // Wrapping around to first card
        val wrapped = manager.nextCard()
        assertEquals(cards[0], wrapped)
    }

    @Test
    fun testPreviousCardCarouselWrapping() {
        val cards = manager.getCards()
        // From index 0, previous card should wrap to last card
        val prev = manager.previousCard()
        assertEquals(cards.last(), prev)

        val prev2 = manager.previousCard()
        assertEquals(cards[cards.size - 2], prev2)
    }

    @Test
    fun testSetActiveCard() {
        val success = manager.setActiveCard(CapsuleCardType.NOTIFICATION)
        assertTrue(success)
        assertEquals(CapsuleCardType.NOTIFICATION, manager.getActiveCard())

        val invalid = manager.setActiveCard(CapsuleCardType.MEDIA)
        assertTrue(invalid)
        assertEquals(CapsuleCardType.MEDIA, manager.getActiveCard())
    }

    @Test
    fun testDisableAndEnableCards() {
        manager.setCardEnabled(CapsuleCardType.TIMER, false)
        assertFalse(manager.isCardEnabled(CapsuleCardType.TIMER))
        assertFalse(manager.getCards().contains(CapsuleCardType.TIMER))
        assertEquals(5, manager.getCards().size)

        manager.setCardEnabled(CapsuleCardType.TIMER, true)
        assertTrue(manager.isCardEnabled(CapsuleCardType.TIMER))
        assertEquals(6, manager.getCards().size)
    }

    @Test
    fun testCannotRemoveLastCard() {
        val customManager = CardStackManager(listOf(CapsuleCardType.MEDIA))
        customManager.removeCard(CapsuleCardType.MEDIA)
        assertEquals(1, customManager.getCards().size)
        assertEquals(CapsuleCardType.MEDIA, customManager.getActiveCard())
    }
}
