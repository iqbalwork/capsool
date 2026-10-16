package com.iqbalwork.capsule.platform.barcode

import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.BarcodeFormat as ZxFormat
import com.iqbalwork.capsule.model.BarcodeFormat

data class BarcodeMatrix(
    val width: Int,
    val height: Int,
    val rows: List<BooleanArray>
) {
    operator fun get(x: Int, y: Int): Boolean = rows[y][x]
}

object BarcodeEncoder {

    fun encode(content: String, format: BarcodeFormat, width: Int = 1, height: Int = 1): BarcodeMatrix {
        val validation = BarcodeValidator.validate(content, format)
        if (validation is BarcodeValidationResult.Error) {
            throw IllegalArgumentException(validation.message)
        }

        val zxingFormat = format.toZxing()
        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val bitMatrix = MultiFormatWriter().encode(
            content,
            zxingFormat,
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            hints
        )

        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val rows = ArrayList<BooleanArray>(matrixHeight)
        for (y in 0 until matrixHeight) {
            val row = BooleanArray(matrixWidth)
            for (x in 0 until matrixWidth) {
                row[x] = bitMatrix.get(x, y)
            }
            rows.add(row)
        }
        return BarcodeMatrix(width = matrixWidth, height = matrixHeight, rows = rows)
    }

    private fun BarcodeFormat.toZxing(): ZxFormat = when (this) {
        BarcodeFormat.QR_CODE -> ZxFormat.QR_CODE
        BarcodeFormat.EAN_13 -> ZxFormat.EAN_13
        BarcodeFormat.EAN_8 -> ZxFormat.EAN_8
        BarcodeFormat.UPC_A -> ZxFormat.UPC_A
        BarcodeFormat.UPC_E -> ZxFormat.UPC_E
        BarcodeFormat.CODE_128 -> ZxFormat.CODE_128
        BarcodeFormat.CODE_39 -> ZxFormat.CODE_39
        BarcodeFormat.CODE_93 -> ZxFormat.CODE_93
        BarcodeFormat.CODABAR -> ZxFormat.CODABAR
        BarcodeFormat.ITF -> ZxFormat.ITF
    }
}
