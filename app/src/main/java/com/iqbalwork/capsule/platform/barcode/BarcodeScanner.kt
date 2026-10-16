package com.iqbalwork.capsule.platform.barcode

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.iqbalwork.capsule.model.BarcodeFormat
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of attempting to decode barcodes from an image. */
sealed interface BarcodeScanResult {
    data class Found(val code: ScannedCode) : BarcodeScanResult
    /** [message] is a human-readable string that can be shown directly in the UI. */
    data class Failed(val message: String) : BarcodeScanResult
}

/**
 * Scans [uri] for barcodes using ML Kit (bundled model — no network required).
 * Never throws; failures are returned as [BarcodeScanResult.Failed].
 * Must not be called on the main thread.
 */
suspend fun scanImage(
    context: Context,
    uri: Uri,
    preferred: BarcodeFormat,
): BarcodeScanResult = withContext(Dispatchers.IO) {
    runCatching {
        val image = InputImage.fromFilePath(context, uri)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF,
            )
            .build()

        val scanner = BarcodeScanning.getClient(options)
        val mlkitBarcodes = try {
            Tasks.await(scanner.process(image))
        } finally {
            scanner.close()
        }

        val codes = mlkitBarcodes.mapNotNull { barcode ->
            val content = barcode.rawValue ?: return@mapNotNull null
            val format = barcode.format.toAppFormat() ?: return@mapNotNull null
            ScannedCode(content, format)
        }

        when (val picked = BarcodeScanPolicy.pick(codes, preferred)) {
            null -> BarcodeScanResult.Failed("No barcode found in that image")
            else -> BarcodeScanResult.Found(picked)
        }
    }.getOrElse { cause ->
        // Only two real failure modes here: the image could not be decoded, or ML Kit itself
        // failed. Both are reported with the underlying cause rather than a generic message.
        val reason = cause.message ?: cause.javaClass.simpleName
        BarcodeScanResult.Failed("Could not read that image: $reason")
    }
}

/**
 * Maps an ML Kit [Barcode.FORMAT_*] constant to our app's [BarcodeFormat].
 * Returns null for formats we do not support (they should be skipped).
 */
private fun Int.toAppFormat(): BarcodeFormat? = when (this) {
    Barcode.FORMAT_QR_CODE  -> BarcodeFormat.QR_CODE
    Barcode.FORMAT_EAN_13   -> BarcodeFormat.EAN_13
    Barcode.FORMAT_EAN_8    -> BarcodeFormat.EAN_8
    Barcode.FORMAT_UPC_A    -> BarcodeFormat.UPC_A
    Barcode.FORMAT_UPC_E    -> BarcodeFormat.UPC_E
    Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
    Barcode.FORMAT_CODE_39  -> BarcodeFormat.CODE_39
    Barcode.FORMAT_CODE_93  -> BarcodeFormat.CODE_93
    Barcode.FORMAT_CODABAR  -> BarcodeFormat.CODABAR
    Barcode.FORMAT_ITF      -> BarcodeFormat.ITF
    else                    -> null
}
