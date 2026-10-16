package com.iqbalwork.capsule.platform.barcode

import com.google.zxing.oned.UPCEReader
import com.iqbalwork.capsule.R
import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.model.BarcodeFormat

sealed interface BarcodeValidationResult {
    data object Ok : BarcodeValidationResult
    data class Error(val message: UiText) : BarcodeValidationResult

    val isValid: Boolean get() = this is Ok
}

object BarcodeValidator {

    private fun fail(@androidx.annotation.StringRes id: Int, vararg args: Any): BarcodeValidationResult =
        BarcodeValidationResult.Error(UiText.res(id, *args))

    private val CODE_39_CHARS = setOf(' ', '-', '.', '$', '/', '+', '%')
    private val CODE_93_BASIC_CHARS = setOf('-', '.', ' ', '$', '/', '+', '%')
    private val CODABAR_DELIMITERS = setOf('A', 'B', 'C', 'D', 'T', 'N', '*', 'E')
    private val CODABAR_INTERIOR_CHARS = setOf('-', '$', ':', '/', '.', '+')

    fun validate(content: String, format: BarcodeFormat): BarcodeValidationResult {
        return when (format) {
            BarcodeFormat.QR_CODE -> validateQr(content)
            BarcodeFormat.EAN_13 -> validateEan13(content)
            BarcodeFormat.EAN_8 -> validateEan8(content)
            BarcodeFormat.UPC_A -> validateUpcA(content)
            BarcodeFormat.UPC_E -> validateUpcE(content)
            BarcodeFormat.ITF -> validateItf(content)
            BarcodeFormat.CODE_39 -> validateCode39(content)
            BarcodeFormat.CODE_93 -> validateCode93(content)
            BarcodeFormat.CODE_128 -> validateCode128(content)
            BarcodeFormat.CODABAR -> validateCodabar(content)
        }
    }

    private fun validateQr(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "QR Code")
        if (content.length > 2953) return fail(R.string.barcode_err_qr_too_long, 2953)
        return BarcodeValidationResult.Ok
    }

    private fun validateEan13(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "EAN-13")
        if (!content.all { it.isDigit() }) return fail(R.string.barcode_err_digits_only, "EAN-13")
        if (content.length != 13) return fail(R.string.barcode_err_digit_count, "EAN-13", 13, content.length)

        var sum = 0
        for (i in 0 until 12) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[12] - '0'
        if (actualCheck != expectedCheck) {
            return fail(R.string.barcode_err_checksum, "EAN-13", actualCheck, expectedCheck)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateEan8(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "EAN-8")
        if (!content.all { it.isDigit() }) return fail(R.string.barcode_err_digits_only, "EAN-8")
        if (content.length != 8) return fail(R.string.barcode_err_digit_count, "EAN-8", 8, content.length)

        var sum = 0
        for (i in 0 until 7) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[7] - '0'
        if (actualCheck != expectedCheck) {
            return fail(R.string.barcode_err_checksum, "EAN-8", actualCheck, expectedCheck)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateUpcA(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "UPC-A")
        if (!content.all { it.isDigit() }) return fail(R.string.barcode_err_digits_only, "UPC-A")
        if (content.length != 12) return fail(R.string.barcode_err_digit_count, "UPC-A", 12, content.length)

        var sum = 0
        for (i in 0 until 11) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[11] - '0'
        if (actualCheck != expectedCheck) {
            return fail(R.string.barcode_err_checksum, "UPC-A", actualCheck, expectedCheck)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateUpcE(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "UPC-E")
        if (!content.all { it.isDigit() }) return fail(R.string.barcode_err_digits_only, "UPC-E")
        if (content.length != 8) return fail(R.string.barcode_err_digit_count, "UPC-E", 8, content.length)
        if (content[0] != '0' && content[0] != '1') {
            return fail(R.string.barcode_err_upce_prefix)
        }

        val upca = runCatching { UPCEReader.convertUPCEtoUPCA(content) }.getOrNull()
            ?: return fail(R.string.barcode_err_upce_data)
        if (upca.length != 12) return fail(R.string.barcode_err_upce_expansion)

        var sum = 0
        for (i in 0 until 11) {
            val digit = upca[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[7] - '0'
        if (actualCheck != expectedCheck) {
            return fail(R.string.barcode_err_checksum, "UPC-E", actualCheck, expectedCheck)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateItf(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "ITF")
        if (!content.all { it.isDigit() }) return fail(R.string.barcode_err_digits_only, "ITF")
        if (content.length % 2 != 0) return fail(R.string.barcode_err_itf_even, content.length)
        if (content.length > 80) return fail(R.string.barcode_err_itf_max, 80, content.length)
        return BarcodeValidationResult.Ok
    }

    private fun validateCode39(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "Code 39")
        val invalidChar = content.firstOrNull { it !in '0'..'9' && it !in 'A'..'Z' && it !in CODE_39_CHARS }
        if (invalidChar != null) {
            return fail(R.string.barcode_err_code39_char, invalidChar)
        }
        if (content.length > 80) return fail(R.string.barcode_err_code39_max, 80, content.length)
        return BarcodeValidationResult.Ok
    }

    private fun validateCode93(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "Code 93")
        val invalidChar = content.firstOrNull { it.code > 127 }
        if (invalidChar != null) {
            return fail(R.string.barcode_err_code93_ascii)
        }
        val encodedLength = content.sumOf { c ->
            if (c in '0'..'9' || c in 'A'..'Z' || c in CODE_93_BASIC_CHARS) 1 else 2
        }
        if (encodedLength > 80) {
            return fail(R.string.barcode_err_code93_max, 80, encodedLength)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateCode128(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "Code 128")
        val invalidChar = content.firstOrNull { it.code > 127 }
        if (invalidChar != null) {
            return fail(R.string.barcode_err_code128_ascii)
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateCodabar(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return fail(R.string.barcode_err_empty, "Codabar")
        if (content.length < 2) return fail(R.string.barcode_err_codabar_min)
        val start = content.first().uppercaseChar()
        val stop = content.last().uppercaseChar()
        if (start !in CODABAR_DELIMITERS || stop !in CODABAR_DELIMITERS) {
            return fail(R.string.barcode_err_codabar_delims)
        }
        for (i in 1 until content.length - 1) {
            val c = content[i]
            if (c !in '0'..'9' && c !in CODABAR_INTERIOR_CHARS) {
                return fail(R.string.barcode_err_codabar_char, c)
            }
        }
        return BarcodeValidationResult.Ok
    }
}
