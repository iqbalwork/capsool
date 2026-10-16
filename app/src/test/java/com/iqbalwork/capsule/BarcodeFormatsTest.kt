package com.iqbalwork.capsule

import com.iqbalwork.capsule.model.BarcodeFormat
import com.iqbalwork.capsule.platform.barcode.SUPPORTED_MLKIT_FORMATS
import com.iqbalwork.capsule.platform.barcode.toAppFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The gallery decoder and the Play Services camera scanner must accept exactly the same set of
 * formats. Both read [SUPPORTED_MLKIT_FORMATS], and these tests fail if that list and
 * [BarcodeFormat] ever drift apart — a format offered in the UI but never requested, or a format
 * requested that the app cannot render a preview for.
 */
class BarcodeFormatsTest {

    @Test
    fun everyRequestedFormatIsOneTheAppSupports() {
        val unmapped = SUPPORTED_MLKIT_FORMATS.filter { it.toAppFormat() == null }
        assertEquals("ML Kit formats requested but not mapped to BarcodeFormat", emptyList<Int>(), unmapped)
    }

    @Test
    fun everyFormatTheAppOffersIsRequestedFromMlKit() {
        val requested = SUPPORTED_MLKIT_FORMATS.toList().mapNotNull { it.toAppFormat() }.toSet()
        val neverRequested = BarcodeFormat.entries.toSet() - requested
        assertEquals("BarcodeFormat values the scanner never asks for", emptySet<BarcodeFormat>(), neverRequested)
    }

    @Test
    fun theFormatListHasNoDuplicates() {
        assertEquals(SUPPORTED_MLKIT_FORMATS.size, SUPPORTED_MLKIT_FORMATS.toSet().size)
    }

    @Test
    fun anUnknownMlKitFormatIsIgnored() {
        assertNull(9999.toAppFormat())
    }
}
