package com.iqbalwork.capsule.platform.barcode

import com.iqbalwork.capsule.model.BarcodeFormat

/** A single barcode read from an image. */
data class ScannedCode(val content: String, val format: BarcodeFormat)

/**
 * Pure selection logic — no Android types, fully testable on the JVM.
 *
 * Rule: prefer the first code whose format matches [preferred];
 * if none matches, fall back to the first non-blank code;
 * codes with blank/empty content are always ignored.
 */
object BarcodeScanPolicy {

    /**
     * Picks the best code from [codes]:
     * - Codes with blank content are discarded.
     * - If any remaining code has [format] == [preferred], the first such code wins.
     * - Otherwise, the first remaining code is returned.
     * - Returns null when [codes] is empty or all codes have blank content.
     */
    fun pick(codes: List<ScannedCode>, preferred: BarcodeFormat): ScannedCode? {
        val nonBlank = codes.filter { it.content.isNotBlank() }
        return nonBlank.firstOrNull { it.format == preferred } ?: nonBlank.firstOrNull()
    }
}
