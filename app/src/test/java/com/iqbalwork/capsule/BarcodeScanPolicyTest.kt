package com.iqbalwork.capsule

import com.iqbalwork.capsule.model.BarcodeFormat
import com.iqbalwork.capsule.platform.barcode.BarcodeScanPolicy
import com.iqbalwork.capsule.platform.barcode.ScannedCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeScanPolicyTest {

    // --- empty list → null ---

    @Test
    fun emptyListReturnsNull() {
        val result = BarcodeScanPolicy.pick(emptyList(), BarcodeFormat.QR_CODE)
        assertNull(result)
    }

    // --- preferred format wins ---

    @Test
    fun preferredFormatWinsEvenIfNotFirst() {
        val codes = listOf(
            ScannedCode("12345678", BarcodeFormat.EAN_8),
            ScannedCode("4006381333931", BarcodeFormat.EAN_13),
        )
        val result = BarcodeScanPolicy.pick(codes, BarcodeFormat.EAN_13)
        assertEquals(BarcodeFormat.EAN_13, result?.format)
        assertEquals("4006381333931", result?.content)
    }

    // --- no preferred match → first code returned ---

    @Test
    fun noMatchFallsBackToFirst() {
        val codes = listOf(
            ScannedCode("036000291452", BarcodeFormat.UPC_A),
            ScannedCode("Capsool-128", BarcodeFormat.CODE_128),
        )
        val result = BarcodeScanPolicy.pick(codes, BarcodeFormat.QR_CODE)
        assertEquals(BarcodeFormat.UPC_A, result?.format)
        assertEquals("036000291452", result?.content)
    }

    // --- blank / whitespace-only content is ignored ---

    @Test
    fun blankContentCodesAreIgnored() {
        val codes = listOf(
            ScannedCode("", BarcodeFormat.QR_CODE),
            ScannedCode("   ", BarcodeFormat.EAN_13),
            ScannedCode("A123456B", BarcodeFormat.CODABAR),
        )
        val result = BarcodeScanPolicy.pick(codes, BarcodeFormat.QR_CODE)
        // QR_CODE is preferred but its content is blank → ignored.
        // EAN_13 content is all spaces → ignored.
        // CODABAR is the only non-blank, so it wins.
        assertEquals(BarcodeFormat.CODABAR, result?.format)
        assertEquals("A123456B", result?.content)
    }

    @Test
    fun allBlankContentReturnsNull() {
        val codes = listOf(
            ScannedCode("", BarcodeFormat.QR_CODE),
            ScannedCode("  ", BarcodeFormat.CODE_128),
        )
        val result = BarcodeScanPolicy.pick(codes, BarcodeFormat.QR_CODE)
        assertNull(result)
    }

    // --- multiple codes with same preferred format → first one wins ---

    @Test
    fun multiplePreferredFormatFirstOneWins() {
        val codes = listOf(
            ScannedCode("https://first.example.com", BarcodeFormat.QR_CODE),
            ScannedCode("https://second.example.com", BarcodeFormat.QR_CODE),
            ScannedCode("036000291452", BarcodeFormat.UPC_A),
        )
        val result = BarcodeScanPolicy.pick(codes, BarcodeFormat.QR_CODE)
        assertEquals("https://first.example.com", result?.content)
    }
}
