package com.iqbalwork.capsule.model

enum class BarcodeFormat(val displayName: String) {
    QR_CODE("QR Code"),
    EAN_13("EAN-13"),
    EAN_8("EAN-8"),
    UPC_A("UPC-A"),
    UPC_E("UPC-E"),
    CODE_128("Code 128"),
    CODE_39("Code 39"),
    CODE_93("Code 93"),
    CODABAR("Codabar"),
    ITF("ITF")
}

data class BarcodeContent(
    val content: String = "",
    val format: BarcodeFormat = BarcodeFormat.QR_CODE,
    val label: String = "",
    val color: Long = 0xFF000000,
    val flipped: Boolean = false,
    val enabled: Boolean = false
)
