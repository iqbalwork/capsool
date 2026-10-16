package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbalwork.capsule.core.settings.DefaultCardPriority
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.settings.moveCard
import com.iqbalwork.capsule.core.settings.normalizeCardPriority
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.CardPage
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Test

/** F-06: the user's card order decides which active card comes first. */
class CardPriorityTest {

    private val N = CapsuleCardType.NOTIFICATION
    private val T = CapsuleCardType.TIMER
    private val M = CapsuleCardType.MEDIA

    @Test
    fun defaultOrderIsTheLongStandingOne() {
        assertEquals(
            listOf(N, T, M, CapsuleCardType.PROGRESS, CapsuleCardType.CHARGING, CapsuleCardType.SLIDERS, CapsuleCardType.SHORTCUTS),
            DefaultCardPriority
        )
    }

    @Test
    fun normalizeDropsDuplicatesAndAppendsMissingCardsInDefaultOrder() {
        val result = normalizeCardPriority(listOf(M, M, N))
        assertEquals(listOf(M, N), result.take(2))
        assertEquals(DefaultCardPriority.size, result.size)
        assertEquals(DefaultCardPriority.toSet(), result.toSet())
        // The cards that were missing keep their default relative order.
        assertEquals(DefaultCardPriority.filter { it != M && it != N }, result.drop(2))
    }

    @Test
    fun moveSwapsWithTheNeighbour() {
        assertEquals(listOf(T, N, M), moveCard(DefaultCardPriority, N, 1).take(3))
        assertEquals(listOf(T, N, M), moveCard(DefaultCardPriority, T, -1).take(3))
    }

    @Test
    fun moveAtEitherEndChangesNothing() {
        assertEquals(DefaultCardPriority, moveCard(DefaultCardPriority, N, -1))
        assertEquals(DefaultCardPriority, moveCard(DefaultCardPriority, CapsuleCardType.CHARGING, 1))
    }

    @Test
    fun controlsAndShortcutsCannotBeMovedAndStayBehindTheOthers() {
        assertEquals(DefaultCardPriority, moveCard(DefaultCardPriority, CapsuleCardType.SLIDERS, -1))
        val moved = moveCard(DefaultCardPriority, CapsuleCardType.CHARGING, -4)
        assertEquals(CapsuleCardType.CHARGING, moved.first())
        assertEquals(listOf(CapsuleCardType.SLIDERS, CapsuleCardType.SHORTCUTS), moved.takeLast(2))
    }

    @Test
    fun orderSurvivesStorageAndUnknownNamesAreSkipped() {
        val prefs = mutablePreferencesOf()
        val custom = moveCard(DefaultCardPriority, M, -2)
        SettingsMapper.write(prefs, Settings(cardPriority = custom))
        assertEquals(custom, SettingsMapper.read(prefs).cardPriority)

        val withJunk = mutablePreferencesOf(stringPreferencesKey("cards.priority") to "FUTURE_CARD,MEDIA")
        val read = SettingsMapper.read(withJunk).cardPriority
        assertEquals(M, read.first())
        assertEquals(DefaultCardPriority.toSet(), read.toSet())
    }

    @Test
    fun missingKeyGivesTheDefaultOrder() {
        assertEquals(DefaultCardPriority, SettingsMapper.read(mutablePreferencesOf()).cardPriority)
    }

    @Test
    fun carouselPagesFollowTheUsersOrder() {
        val timer = TimerState(
            key = "t", packageName = "clock", appName = "Clock", label = "Timer",
            isCountdown = true, baseTimeMs = 60_000L, isRunning = true
        )
        fun pagesWith(order: List<CapsuleCardType>) = CapsuleState(
            settingsLoaded = true,
            settings = Settings(cardPriority = order, chargingCardEnabled = true),
            timer = timer,
            charging = com.iqbalwork.capsule.model.ChargingState(isCharging = true)
        ).pages.filterIsInstance<CardPage.Dynamic>().map { it.type }

        assertEquals(listOf(T, CapsuleCardType.CHARGING), pagesWith(DefaultCardPriority))
        assertEquals(
            listOf(CapsuleCardType.CHARGING, T),
            pagesWith(moveCard(DefaultCardPriority, CapsuleCardType.CHARGING, -3))
        )
    }
}
