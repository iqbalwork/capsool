package com.iqbalwork.capsule.platform.barcode

import com.google.zxing.oned.UPCEReader
import com.iqbalwork.capsule.model.BarcodeFormat

sealed interface BarcodeValidationResult {
    data object Ok : BarcodeValidationResult
    data class Error(val message: String) : BarcodeValidationResult

    val isValid: Boolean get() = this is Ok
}

object BarcodeValidator {

    private val CODE_39_CHARS = setOf(' ', '-', '.', '$', '/', '+', '%')
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
        if (content.isEmpty()) return BarcodeValidationResult.Error("QR Code tidak boleh kosong")
        if (content.length > 2953) return BarcodeValidationResult.Error("QR Code melebihi batas 2953 karakter")
        return BarcodeValidationResult.Ok
    }

    private fun validateEan13(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("EAN-13 tidak boleh kosong")
        if (!content.all { it.isDigit() }) return BarcodeValidationResult.Error("EAN-13 hanya menerima angka")
        if (content.length != 13) return BarcodeValidationResult.Error("EAN-13 butuh 13 digit angka (diberikan ${content.length})")

        var sum = 0
        for (i in 0 until 12) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[12] - '0'
        if (actualCheck != expectedCheck) {
            return BarcodeValidationResult.Error("EAN-13 checksum salah (ditemukan $actualCheck, harusnya $expectedCheck)")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateEan8(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("EAN-8 tidak boleh kosong")
        if (!content.all { it.isDigit() }) return BarcodeValidationResult.Error("EAN-8 hanya menerima angka")
        if (content.length != 8) return BarcodeValidationResult.Error("EAN-8 butuh 8 digit angka (diberikan ${content.length})")

        var sum = 0
        for (i in 0 until 7) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[7] - '0'
        if (actualCheck != expectedCheck) {
            return BarcodeValidationResult.Error("EAN-8 checksum salah (ditemukan $actualCheck, harusnya $expectedCheck)")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateUpcA(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("UPC-A tidak boleh kosong")
        if (!content.all { it.isDigit() }) return BarcodeValidationResult.Error("UPC-A hanya menerima angka")
        if (content.length != 12) return BarcodeValidationResult.Error("UPC-A butuh 12 digit angka (diberikan ${content.length})")

        var sum = 0
        for (i in 0 until 11) {
            val digit = content[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[11] - '0'
        if (actualCheck != expectedCheck) {
            return BarcodeValidationResult.Error("UPC-A checksum salah (ditemukan $actualCheck, harusnya $expectedCheck)")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateUpcE(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("UPC-E tidak boleh kosong")
        if (!content.all { it.isDigit() }) return BarcodeValidationResult.Error("UPC-E hanya menerima angka")
        if (content.length != 8) return BarcodeValidationResult.Error("UPC-E butuh 8 digit angka (diberikan ${content.length})")
        if (content[0] != '0' && content[0] != '1') {
            return BarcodeValidationResult.Error("UPC-E harus diawali dengan digit 0 atau 1")
        }

        val upca = runCatching { UPCEReader.convertUPCEtoUPCA(content) }.getOrNull()
            ?: return BarcodeValidationResult.Error("UPC-E format data tidak valid")
        if (upca.length != 12) return BarcodeValidationResult.Error("UPC-E format ekspansi tidak valid")

        var sum = 0
        for (i in 0 until 11) {
            val digit = upca[i] - '0'
            sum += if (i % 2 == 0) digit * 3 else digit
        }
        val expectedCheck = (10 - (sum % 10)) % 10
        val actualCheck = content[7] - '0'
        if (actualCheck != expectedCheck) {
            return BarcodeValidationResult.Error("UPC-E checksum salah (ditemukan $actualCheck, harusnya $expectedCheck)")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateItf(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("ITF tidak boleh kosong")
        if (!content.all { it.isDigit() }) return BarcodeValidationResult.Error("ITF hanya menerima angka")
        if (content.length % 2 != 0) return BarcodeValidationResult.Error("ITF butuh panjang digit genap (diberikan ${content.length})")
        return BarcodeValidationResult.Ok
    }

    private fun validateCode39(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("Code 39 tidak boleh kosong")
        val invalidChar = content.firstOrNull { it !in '0'..'9' && it !in 'A'..'Z' && it !in CODE_39_CHARS }
        if (invalidChar != null) {
            return BarcodeValidationResult.Error("Code 39 karakter tidak valid: '$invalidChar'. Hanya mendukung 0-9, A-Z, dan - . $ / + % spasi")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateCode93(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("Code 93 tidak boleh kosong")
        val invalidChar = content.firstOrNull { it.code > 127 }
        if (invalidChar != null) {
            return BarcodeValidationResult.Error("Code 93 hanya mendukung karakter ASCII")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateCode128(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("Code 128 tidak boleh kosong")
        val invalidChar = content.firstOrNull { it.code > 127 }
        if (invalidChar != null) {
            return BarcodeValidationResult.Error("Code 128 hanya mendukung karakter ASCII")
        }
        return BarcodeValidationResult.Ok
    }

    private fun validateCodabar(content: String): BarcodeValidationResult {
        if (content.isEmpty()) return BarcodeValidationResult.Error("Codabar tidak boleh kosong")
        if (content.length < 2) return BarcodeValidationResult.Error("Codabar minimal 2 karakter")
        val start = content.first().uppercaseChar()
        val stop = content.last().uppercaseChar()
        if (start !in CODABAR_DELIMITERS || stop !in CODABAR_DELIMITERS) {
            return BarcodeValidationResult.Error("Codabar harus diawali dan diakhiri dengan karakter A, B, C, atau D")
        }
        for (i in 1 until content.length - 1) {
            val c = content[i]
            if (c !in '0'..'9' && c !in CODABAR_INTERIOR_CHARS) {
                return BarcodeValidationResult.Error("Codabar karakter isi tidak valid: '$c'. Hanya mendukung 0-9 dan - $ : / . +")
            }
        }
        return BarcodeValidationResult.Ok
    }
}
