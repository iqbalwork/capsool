package com.iqbalwork.capsule

import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType
import com.iqbalwork.capsule.core.settings.DefaultIdleGestures
import com.iqbalwork.capsule.core.settings.GestureAction
import com.iqbalwork.capsule.core.settings.GestureBinding
import com.iqbalwork.capsule.core.settings.GestureType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.StaticCard
import com.iqbalwork.capsule.core.settings.StaticCardType
import com.iqbalwork.capsule.core.settings.StaticCardViewMode
import com.iqbalwork.capsule.core.store.CapsuleReducer
import com.iqbalwork.capsule.core.store.CapsuleState
import com.iqbalwork.capsule.core.store.CardPage
import com.iqbalwork.capsule.core.store.Effect
import com.iqbalwork.capsule.core.store.UserIntent
import com.iqbalwork.capsule.model.CapsuleCardType
import com.iqbalwork.capsule.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** M6c-3: static cards as carousel pages, and how they interact with gestures and fokus. */
class StaticCardPagesTest {

    private val shortcutsCard = StaticCard("a", StaticCardType.SHORTCUTS, "Shortcuts")
    private val barcodeCard = StaticCard("b", StaticCardType.BARCODE, "Loyalty")
    private val intentCard = StaticCard("c", StaticCardType.INTENT, "Signals")

    private fun stateWithCards(
        cards: List<StaticCard> = listOf(shortcutsCard, barcodeCard, intentCard),
        startCardId: String? = null,
        customIntents: List<CustomIntent> = emptyList(),
        timer: TimerState? = null,
        viewMode: StaticCardViewMode = StaticCardViewMode.CAROUSEL
    ) = CapsuleState(
        settingsLoaded = true,
        settings = Settings(
            staticCards = cards,
            startCardId = startCardId,
            customIntents = customIntents,
            staticCardViewMode = viewMode,
            gesturesIdle = DefaultIdleGestures +
                (GestureType.SWIPE_LEFT to GestureBinding(GestureAction.NEXT_CARD)) +
                (GestureType.SWIPE_RIGHT to GestureBinding(GestureAction.PREVIOUS_CARD))
        ),
        timer = timer
    )

    private val runningTimer = TimerState(
        key = "t", packageName = "clock", appName = "Clock", label = "Timer",
        isCountdown = true, baseTimeMs = 60_000L, isRunning = true
    )

    private fun reduce(state: CapsuleState, vararg actions: com.iqbalwork.capsule.core.store.Action) =
        actions.fold(state) { s, a -> CapsuleReducer.reduce(s, a).state }

    @Test
    fun staticCardsBecomePagesInUserOrderWhenNothingIsActive() {
        val state = stateWithCards()
        assertEquals(
            listOf(shortcutsCard, barcodeCard, intentCard).map { CardPage.Static(it) },
            state.pages
        )
    }

    @Test
    fun dynamicActivityHidesTheStaticCards() {
        val state = stateWithCards(timer = runningTimer)
        assertEquals(listOf<CardPage>(CardPage.Dynamic(CapsuleCardType.TIMER)), state.pages)
    }

    // --- LIST view (M6c-3b) ---

    @Test
    fun listModeStacksTheCardsWhenNothingIsRunning() {
        val state = stateWithCards(viewMode = StaticCardViewMode.LIST)
        assertTrue(state.showsStaticList)
        assertEquals(3, state.listRows)
    }

    @Test
    fun carouselModeNeverUsesTheList() {
        val state = stateWithCards(viewMode = StaticCardViewMode.CAROUSEL)
        assertFalse(state.showsStaticList)
        assertEquals(0, state.listRows)
    }

    @Test
    fun runningActivityKeepsTheCarouselEvenInListMode() {
        // LIST is the idle presentation (spec line 81); a real activity gets the carousel.
        val state = stateWithCards(timer = runningTimer, viewMode = StaticCardViewMode.LIST)
        assertTrue(state.hasActivity)
        assertFalse(state.showsStaticList)
        assertEquals(0, state.listRows)
    }

    @Test
    fun listRowsFollowTheCardCount() {
        val state = stateWithCards(cards = listOf(shortcutsCard, barcodeCard), viewMode = StaticCardViewMode.LIST)
        assertEquals(2, state.listRows)
    }

    @Test
    fun listIsNotUsedWithoutStaticCards() {
        val state = stateWithCards(cards = emptyList(), viewMode = StaticCardViewMode.LIST)
        assertFalse(state.showsStaticList)
        assertEquals(0, state.listRows)
    }

    @Test
    fun staticCardsAreIdleContextNotActivity() {
        // F-75: gestures must pick the "idle" mapping while a static card is on screen.
        // TAP is the discriminator: active = TOGGLE_EXPAND, idle = OPEN_CONTROLS.
        val state = stateWithCards()
        assertFalse(state.hasActivity)
        assertEquals(GestureAction.OPEN_CONTROLS, state.gesture(GestureType.TAP).action)

        val withTimer = stateWithCards(timer = runningTimer)
        assertTrue(withTimer.hasActivity)
        assertEquals(GestureAction.TOGGLE_EXPAND, withTimer.gesture(GestureType.TAP).action)
    }

    @Test
    fun activePageHonoursTheStartCard() {
        assertEquals(CardPage.Static(shortcutsCard), stateWithCards().activePage)
        assertEquals(CardPage.Static(intentCard), stateWithCards(startCardId = "c").activePage)
    }

    @Test
    fun startCardThatNoLongerExistsFallsBackToTheFirstPage() {
        assertEquals(CardPage.Static(shortcutsCard), stateWithCards(startCardId = "gone").activePage)
    }

    @Test
    fun nextAndPreviousCycleAcrossStaticCards() {
        val state = stateWithCards()
        assertEquals(CardPage.Static(shortcutsCard), state.activePage)

        val second = reduce(state, UserIntent.Gesture(GestureType.SWIPE_LEFT))
        assertEquals(CardPage.Static(barcodeCard), second.activePage)

        val third = reduce(second, UserIntent.Gesture(GestureType.SWIPE_LEFT))
        assertEquals(CardPage.Static(intentCard), third.activePage)

        // Wraps around, matching the dynamic carousel.
        assertEquals(CardPage.Static(shortcutsCard), reduce(third, UserIntent.Gesture(GestureType.SWIPE_LEFT)).activePage)
        assertEquals(CardPage.Static(intentCard), reduce(state, UserIntent.Gesture(GestureType.SWIPE_RIGHT)).activePage)
    }

    @Test
    fun openShortcutsExpandsTheStaticCarousel() {
        // DefaultIdleGestures already maps a long press to OPEN_SHORTCUTS.
        val state = CapsuleState(
            settingsLoaded = true,
            settings = Settings(
                staticCards = listOf(shortcutsCard, barcodeCard, intentCard),
                startCardId = "b"
            )
        )

        val opened = reduce(state, UserIntent.Gesture(GestureType.LONG_PRESS))
        assertTrue("static cards should expand the capsule", opened.isExpanded)
        // It must not fall back to the dynamic Shortcuts card.
        assertFalse(CardPage.Dynamic(CapsuleCardType.SHORTCUTS) in opened.pages)
        assertEquals(CardPage.Static(barcodeCard), opened.activePage)
    }

    @Test
    fun runCustomIntentFiresTheEffectAndCollapses() {
        val intent = CustomIntent("i1", "Toggle", CustomIntentType.BROADCAST, "com.example.ACTION")
        val state = stateWithCards(customIntents = listOf(intent)).copy(isExpanded = true)

        val reduction = CapsuleReducer.reduce(state, UserIntent.RunCustomIntent("i1"))
        assertEquals(listOf(Effect.SendCustomIntent("i1")), reduction.effects)
        assertFalse(reduction.state.isExpanded)
    }

    @Test
    fun runCustomIntentIgnoresAnIntentThatIsNoLongerConfigured() {
        val state = stateWithCards().copy(isExpanded = true)
        val reduction = CapsuleReducer.reduce(state, UserIntent.RunCustomIntent("deleted"))
        assertTrue(reduction.effects.isEmpty())
    }
}
