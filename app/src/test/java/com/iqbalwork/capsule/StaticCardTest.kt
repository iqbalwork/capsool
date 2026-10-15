package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbalwork.capsule.core.settings.ContactAction
import com.iqbalwork.capsule.core.settings.CustomIntent
import com.iqbalwork.capsule.core.settings.CustomIntentType
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.core.settings.Shortcut
import com.iqbalwork.capsule.core.settings.ShortcutKind
import com.iqbalwork.capsule.core.settings.StaticCard
import com.iqbalwork.capsule.core.settings.StaticCardType
import com.iqbalwork.capsule.core.settings.StaticCardViewMode
import com.iqbalwork.capsule.core.settings.moveDown
import com.iqbalwork.capsule.core.settings.moveUp
import com.iqbalwork.capsule.core.settings.without
import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.model.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticCardTest {

    // --- Encode / Decode Round-Trip ---

    @Test
    fun roundTripEmptyList() {
        val encoded = StaticCard.encodeAll(emptyList())
        assertEquals("", encoded)
        assertTrue(StaticCard.decodeAll(encoded).isEmpty())
        assertTrue(StaticCard.decodeAll(null).isEmpty())
    }

    @Test
    fun roundTripSingleCard() {
        val cards = listOf(
            StaticCard("sc-1", StaticCardType.SHORTCUTS, "My Shortcuts")
        )
        val encoded = StaticCard.encodeAll(cards)
        val decoded = StaticCard.decodeAll(encoded)
        assertEquals(cards, decoded)
    }

    @Test
    fun roundTripMultipleCards() {
        val cards = listOf(
            StaticCard("sc-1", StaticCardType.SHORTCUTS, "Pintasan"),
            StaticCard("bc-1", StaticCardType.BARCODE, "Member Card"),
            StaticCard("sl-1", StaticCardType.SLIDERS, "Volume & Kecerahan"),
            StaticCard("in-1", StaticCardType.INTENT, "Kirim Sinyal")
        )
        val encoded = StaticCard.encodeAll(cards)
        val decoded = StaticCard.decodeAll(encoded)
        assertEquals(cards, decoded)
    }

    @Test
    fun labelContainingControlCharactersSanitizedSafely() {
        val dirtyCard = StaticCard("card-1", StaticCardType.SHORTCUTS, "Short\u001Fcuts\u001EHeader")
        val encoded = StaticCard.encodeAll(listOf(dirtyCard))
        val decoded = StaticCard.decodeAll(encoded)

        assertEquals(1, decoded.size)
        assertEquals("card-1", decoded[0].id)
        assertEquals(StaticCardType.SHORTCUTS, decoded[0].type)
        assertEquals("ShortcutsHeader", decoded[0].label)
    }

    @Test
    fun corruptedRecordsSkippedWithoutCrashing() {
        assertTrue(StaticCard.decodeAll(null).isEmpty())
        assertTrue(StaticCard.decodeAll("").isEmpty())
        assertTrue(StaticCard.decodeAll("   ").isEmpty())
        assertTrue(StaticCard.decodeAll("corrupted_record_without_delimiters").isEmpty())
        // Too few fields
        assertTrue(StaticCard.decodeAll("id\u001FSHORTCUTS").isEmpty())
        // Unknown type
        assertTrue(StaticCard.decodeAll("id\u001FUNKNOWN_TYPE\u001FLabel").isEmpty())
        // Empty ID
        assertTrue(StaticCard.decodeAll("\u001FSHORTCUTS\u001FLabel").isEmpty())

        // Mixed valid and corrupted: returns valid one
        val valid = StaticCard("valid-1", StaticCardType.BARCODE, "Barcode")
        val mixed = "garbage\u001E" + StaticCard.encodeAll(listOf(valid)) + "\u001Einvalid\u001FTOO_FEW"
        val decoded = StaticCard.decodeAll(mixed)
        assertEquals(listOf(valid), decoded)
    }

    // --- Order & Manipulation Helpers ---

    @Test
    fun moveUpBoundaryAndMissingId() {
        val cards = listOf(
            StaticCard("1", StaticCardType.SHORTCUTS, "First"),
            StaticCard("2", StaticCardType.BARCODE, "Second"),
            StaticCard("3", StaticCardType.SLIDERS, "Third")
        )

        // Move first item up -> no-op
        assertEquals(cards, cards.moveUp("1"))
        // Move missing id up -> no-op
        assertEquals(cards, cards.moveUp("non-existent"))

        // Move second item up -> swapped with first
        val moved = cards.moveUp("2")
        assertEquals(listOf("2", "1", "3"), moved.map { it.id })
    }

    @Test
    fun moveDownBoundaryAndMissingId() {
        val cards = listOf(
            StaticCard("1", StaticCardType.SHORTCUTS, "First"),
            StaticCard("2", StaticCardType.BARCODE, "Second"),
            StaticCard("3", StaticCardType.SLIDERS, "Third")
        )

        // Move last item down -> no-op
        assertEquals(cards, cards.moveDown("3"))
        // Move missing id down -> no-op
        assertEquals(cards, cards.moveDown("non-existent"))

        // Move second item down -> swapped with third
        val moved = cards.moveDown("2")
        assertEquals(listOf("1", "3", "2"), moved.map { it.id })
    }

    @Test
    fun withoutRemovesCardAndCleansEffectiveStartCardId() {
        val cards = listOf(
            StaticCard("c1", StaticCardType.SHORTCUTS, "Shortcuts"),
            StaticCard("c2", StaticCardType.BARCODE, "Barcode")
        )

        val updated = cards.without("c1")
        assertEquals(listOf("c2"), updated.map { it.id })
        // Removing non-existent id -> unchanged
        assertEquals(cards, cards.without("non-existent"))

        // When startCardId points to removed card, effectiveStartCardId becomes null
        val settings = Settings(staticCards = cards, startCardId = "c1")
        assertEquals("c1", settings.effectiveStartCardId)

        val afterRemoved = settings.copy(staticCards = settings.staticCards.without("c1"))
        assertNull("startCardId pointing to removed card must be cleaned to null", afterRemoved.effectiveStartCardId)
    }

    // --- Effective Static Cards & Backward Compatibility ---

    @Test
    fun effectiveStaticCardsWhenStaticCardsIsNotEmpty() {
        val userCards = listOf(
            StaticCard("u-1", StaticCardType.INTENT, "Action"),
            StaticCard("u-2", StaticCardType.SHORTCUTS, "Pintasan")
        )
        val settings = Settings(
            staticCards = userCards,
            shortcuts = listOf(Shortcut("1", ShortcutKind.APP, "App", "com.example")),
            barcodeCardEnabled = true,
            barcode = BarcodeContent(content = "https://example.com", format = BarcodeFormat.QR_CODE)
        )
        // User-defined staticCards must be preserved as-is
        assertEquals(userCards, settings.effectiveStaticCards)
    }

    @Test
    fun effectiveStaticCardsWhenEmptyFallsBackToLegacySettings() {
        // All empty -> emptyList()
        val allEmpty = Settings()
        assertTrue(allEmpty.effectiveStaticCards.isEmpty())

        // Only shortcuts
        val withShortcuts = Settings(
            shortcuts = listOf(Shortcut("s1", ShortcutKind.APP, "App", "com.example"))
        )
        assertEquals(
            listOf(StaticCard("default-shortcuts", StaticCardType.SHORTCUTS, "Shortcuts")),
            withShortcuts.effectiveStaticCards
        )

        // Only barcode (valid QR code)
        val withBarcode = Settings(
            barcodeCardEnabled = true,
            barcode = BarcodeContent(content = "https://example.com", format = BarcodeFormat.QR_CODE)
        )
        assertEquals(
            listOf(StaticCard("default-barcode", StaticCardType.BARCODE, "Barcode")),
            withBarcode.effectiveStaticCards
        )

        // Barcode with invalid content -> not included
        val withInvalidBarcode = Settings(
            barcodeCardEnabled = true,
            barcode = BarcodeContent(content = "", format = BarcodeFormat.QR_CODE)
        )
        assertTrue(withInvalidBarcode.effectiveStaticCards.isEmpty())

        // Barcode disabled -> not included even if content valid
        val withDisabledBarcode = Settings(
            barcodeCardEnabled = false,
            barcode = BarcodeContent(content = "https://example.com", format = BarcodeFormat.QR_CODE)
        )
        assertTrue(withDisabledBarcode.effectiveStaticCards.isEmpty())

        // Only custom intents
        val withIntents = Settings(
            customIntents = listOf(CustomIntent("i1", "Trigger", CustomIntentType.BROADCAST, "com.action"))
        )
        assertEquals(
            listOf(StaticCard("default-intent", StaticCardType.INTENT, "Custom action")),
            withIntents.effectiveStaticCards
        )

        // All legacy enabled: order must be SHORTCUTS -> BARCODE -> INTENT
        val allLegacy = Settings(
            shortcuts = listOf(Shortcut("s1", ShortcutKind.APP, "App", "com.example")),
            barcodeCardEnabled = true,
            barcode = BarcodeContent(content = "https://example.com", format = BarcodeFormat.QR_CODE),
            customIntents = listOf(CustomIntent("i1", "Trigger", CustomIntentType.BROADCAST, "com.action"))
        )
        assertEquals(
            listOf(
                StaticCard("default-shortcuts", StaticCardType.SHORTCUTS, "Shortcuts"),
                StaticCard("default-barcode", StaticCardType.BARCODE, "Barcode"),
                StaticCard("default-intent", StaticCardType.INTENT, "Custom action")
            ),
            allLegacy.effectiveStaticCards
        )
    }

    // --- Effective Start Card ID ---

    @Test
    fun effectiveStartCardIdValidReturnsIdInvalidReturnsNull() {
        val userCards = listOf(
            StaticCard("c-1", StaticCardType.SHORTCUTS, "Shortcuts"),
            StaticCard("c-2", StaticCardType.SLIDERS, "Controls")
        )

        val validStart = Settings(staticCards = userCards, startCardId = "c-2")
        assertEquals("c-2", validStart.effectiveStartCardId)

        val invalidStart = Settings(staticCards = userCards, startCardId = "unknown-id")
        assertNull(invalidStart.effectiveStartCardId)

        val nullStart = Settings(staticCards = userCards, startCardId = null)
        assertNull(nullStart.effectiveStartCardId)

        // Works with derived legacy cards too
        val legacySettings = Settings(
            shortcuts = listOf(Shortcut("s1", ShortcutKind.APP, "App", "com.example")),
            startCardId = "default-shortcuts"
        )
        assertEquals("default-shortcuts", legacySettings.effectiveStartCardId)

        val legacyInvalidStart = Settings(
            shortcuts = listOf(Shortcut("s1", ShortcutKind.APP, "App", "com.example")),
            startCardId = "c-missing"
        )
        assertNull(legacyInvalidStart.effectiveStartCardId)
    }

    // --- Persistence in SettingsMapper ---

    @Test
    fun settingsMapperPersistsStaticCardsAndStartIdAndViewMode() {
        val original = Settings(
            staticCards = listOf(
                StaticCard("sc-1", StaticCardType.SHORTCUTS, "Pintasan"),
                StaticCard("sl-1", StaticCardType.SLIDERS, "Slider")
            ),
            startCardId = "sl-1",
            staticCardViewMode = StaticCardViewMode.LIST
        )

        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, original)
        val restored = SettingsMapper.read(prefs)

        assertEquals(original.staticCards, restored.staticCards)
        assertEquals(original.startCardId, restored.startCardId)
        assertEquals(original.staticCardViewMode, restored.staticCardViewMode)
    }

    @Test
    fun settingsMapperHandlesUnknownViewModeAndEmptyStartCard() {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("cards.staticViewMode") to "UNKNOWN_MODE",
            stringPreferencesKey("cards.staticStart") to ""
        )
        val restored = SettingsMapper.read(prefs)

        assertEquals(StaticCardViewMode.CAROUSEL, restored.staticCardViewMode)
        assertNull(restored.startCardId)
        assertTrue(restored.staticCards.isEmpty())
    }
}
