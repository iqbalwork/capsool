package com.iqbalwork.capsule

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.iqbalwork.capsule.core.settings.Settings
import com.iqbalwork.capsule.core.settings.SettingsMapper
import com.iqbalwork.capsule.model.BarcodeContent
import com.iqbalwork.capsule.model.BarcodeFormat
import com.iqbalwork.capsule.platform.barcode.BarcodeEncoder
import com.iqbalwork.capsule.platform.barcode.BarcodeValidationResult
import com.iqbalwork.capsule.platform.barcode.BarcodeValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BarcodeTest {

    // --- Validator: QR Code ---

    @Test
    fun qrCodeValid() {
        val res = BarcodeValidator.validate("https://example.com/capsule", BarcodeFormat.QR_CODE)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun qrCodeEmpty() {
        val res = BarcodeValidator.validate("", BarcodeFormat.QR_CODE)
        assertTrue(res is BarcodeValidationResult.Error)
        val error = res as BarcodeValidationResult.Error
        assertTrue(error.message.contains("QR Code"))
    }

    // --- Validator: EAN-13 ---

    @Test
    fun ean13Valid() {
        // 4006381333931 is valid EAN-13 (Stabilo Boss highlighter)
        val res = BarcodeValidator.validate("4006381333931", BarcodeFormat.EAN_13)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun ean13InvalidChecksum() {
        val res = BarcodeValidator.validate("4006381333932", BarcodeFormat.EAN_13)
        assertTrue(res is BarcodeValidationResult.Error)
        val error = res as BarcodeValidationResult.Error
        assertTrue(error.message.lowercase().contains("checksum"))
    }

    @Test
    fun ean13InvalidLength() {
        val res12 = BarcodeValidator.validate("400638133393", BarcodeFormat.EAN_13)
        assertTrue(res12 is BarcodeValidationResult.Error)

        val res14 = BarcodeValidator.validate("40063813339310", BarcodeFormat.EAN_13)
        assertTrue(res14 is BarcodeValidationResult.Error)
    }

    @Test
    fun ean13NonDigits() {
        val res = BarcodeValidator.validate("400638133393A", BarcodeFormat.EAN_13)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: EAN-8 ---

    @Test
    fun ean8Valid() {
        val res = BarcodeValidator.validate("96385074", BarcodeFormat.EAN_8)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun ean8InvalidChecksum() {
        val res = BarcodeValidator.validate("96385075", BarcodeFormat.EAN_8)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    @Test
    fun ean8InvalidLength() {
        val res = BarcodeValidator.validate("9638507", BarcodeFormat.EAN_8)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: UPC-A ---

    @Test
    fun upcaValid() {
        val res = BarcodeValidator.validate("036000291452", BarcodeFormat.UPC_A)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun upcaInvalidChecksum() {
        val res = BarcodeValidator.validate("036000291450", BarcodeFormat.UPC_A)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    @Test
    fun upcaInvalidLength() {
        val res = BarcodeValidator.validate("03600029145", BarcodeFormat.UPC_A)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: UPC-E ---

    @Test
    fun upceValid() {
        // "01234565" is valid 8-digit UPC-E
        val res = BarcodeValidator.validate("01234565", BarcodeFormat.UPC_E)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun upceInvalidChecksum() {
        val res = BarcodeValidator.validate("01234564", BarcodeFormat.UPC_E)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    @Test
    fun upceInvalidLength() {
        val res = BarcodeValidator.validate("012345", BarcodeFormat.UPC_E)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: ITF ---

    @Test
    fun itfValidEvenLength() {
        val res = BarcodeValidator.validate("123456", BarcodeFormat.ITF)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun itfInvalidOddLength() {
        val res = BarcodeValidator.validate("12345", BarcodeFormat.ITF)
        assertTrue(res is BarcodeValidationResult.Error)
        val error = res as BarcodeValidationResult.Error
        assertTrue(error.message.lowercase().contains("genap"))
    }

    @Test
    fun itfNonDigits() {
        val res = BarcodeValidator.validate("12345A", BarcodeFormat.ITF)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: Code 39 ---

    @Test
    fun code39Valid() {
        val res = BarcodeValidator.validate("CODE 39-TEST.1", BarcodeFormat.CODE_39)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun code39Invalid() {
        val res = BarcodeValidator.validate("code39", BarcodeFormat.CODE_39) // lowercase not permitted
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Validator: Code 93 ---

    @Test
    fun code93Valid() {
        val res = BarcodeValidator.validate("TEST93-A", BarcodeFormat.CODE_93)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    // --- Validator: Code 128 ---

    @Test
    fun code128Valid() {
        val res = BarcodeValidator.validate("Capsool Code 128!", BarcodeFormat.CODE_128)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    // --- Validator: Codabar ---

    @Test
    fun codabarValid() {
        val res = BarcodeValidator.validate("A123456B", BarcodeFormat.CODABAR)
        assertTrue(res is BarcodeValidationResult.Ok)
    }

    @Test
    fun codabarInvalidWithoutStartStop() {
        val res = BarcodeValidator.validate("123456", BarcodeFormat.CODABAR)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Encoder: Pure Matrix ---

    @Test
    fun encodeQrCodeReturnsSquareMatrix() {
        val matrix = BarcodeEncoder.encode("https://example.com", BarcodeFormat.QR_CODE)
        assertNotNull(matrix)
        assertTrue(matrix.width > 0)
        assertEquals(matrix.width, matrix.height)
        assertEquals(matrix.height, matrix.rows.size)
        assertEquals(matrix.width, matrix.rows[0].size)
    }

    @Test
    fun encodeEan13ReturnsExpectedModules() {
        val matrix = BarcodeEncoder.encode("4006381333931", BarcodeFormat.EAN_13)
        assertNotNull(matrix)
        // Standard EAN-13 has 95 modules (3 guard + 42 left + 5 center + 42 right + 3 guard)
        assertEquals(95, matrix.width)
        assertTrue(matrix.height >= 1)
    }

    @Test
    fun encodeInvalidContentThrows() {
        try {
            BarcodeEncoder.encode("invalid-checksum", BarcodeFormat.EAN_13)
            fail("Expected IllegalArgumentException for invalid barcode content")
        } catch (expected: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun codabarInvalidInteriorCharacters() {
        val res = BarcodeValidator.validate("A12X45B", BarcodeFormat.CODABAR)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    @Test
    fun qrCodeTooLong() {
        val longText = "A".repeat(3000)
        val res = BarcodeValidator.validate(longText, BarcodeFormat.QR_CODE)
        assertTrue(res is BarcodeValidationResult.Error)
    }

    // --- Encoder: Other Formats & Matrix Indexing ---

    @Test
    fun encodeCode128ReturnsMatrix() {
        val matrix = BarcodeEncoder.encode("Capsool-128", BarcodeFormat.CODE_128)
        assertNotNull(matrix)
        assertTrue(matrix.width > 0)
        assertTrue(matrix.height >= 1)
        // Check indexer operator get(x, y)
        val pixel = matrix[0, 0]
        assertTrue(pixel == true || pixel == false)
    }

    @Test
    fun encodeItfReturnsMatrix() {
        val matrix = BarcodeEncoder.encode("123456", BarcodeFormat.ITF)
        assertNotNull(matrix)
        assertTrue(matrix.width > 0)
    }

    @Test
    fun encodeUpcaReturnsMatrix() {
        val matrix = BarcodeEncoder.encode("036000291452", BarcodeFormat.UPC_A)
        assertNotNull(matrix)
        assertTrue(matrix.width > 0)
    }

    @Test
    fun barcodeContentDefaults() {
        val default = BarcodeContent()
        assertEquals("", default.content)
        assertEquals(BarcodeFormat.QR_CODE, default.format)
        assertEquals("", default.label)
        assertEquals(0xFF000000L, default.color)
        assertFalse(default.flipped)
        assertFalse(default.enabled)
    }

    // --- Settings Mapper: BarcodeContent ---

    @Test
    fun settingsRoundTripWithBarcode() {
        val barcode = BarcodeContent(
            content = "4006381333931",
            format = BarcodeFormat.EAN_13,
            label = "Member Card",
            color = 0xFF123456,
            flipped = true,
            enabled = true
        )
        val settings = Settings(
            barcode = barcode,
            barcodeCardEnabled = true
        )
        val prefs = mutablePreferencesOf()
        SettingsMapper.write(prefs, settings)
        val readBack = SettingsMapper.read(prefs)
        assertEquals(barcode, readBack.barcode)
        assertTrue(readBack.barcodeCardEnabled)
    }

    @Test
    fun settingsReadOldPrefsGivesDefaultBarcode() {
        val emptyPrefs = mutablePreferencesOf()
        val readBack = SettingsMapper.read(emptyPrefs)
        assertEquals(BarcodeContent(), readBack.barcode)
        assertTrue(readBack.barcodeCardEnabled)
    }
}
