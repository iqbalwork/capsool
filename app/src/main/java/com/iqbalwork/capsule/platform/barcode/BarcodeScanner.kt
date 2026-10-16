package com.iqbalwork.capsule.platform.barcode

import android.app.Activity
import com.iqbalwork.capsule.core.UiText
import com.iqbalwork.capsule.R
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.iqbalwork.capsule.model.BarcodeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Result of attempting to decode a barcode. */
sealed interface BarcodeScanResult {
    data class Found(val code: ScannedCode) : BarcodeScanResult
    /** [message] is shown directly in the UI, translated at display time. */
    data class Failed(val message: UiText) : BarcodeScanResult
    /** The user backed out of the camera scanner. Not an error — show nothing. */
    data object Cancelled : BarcodeScanResult
}

/**
 * The ML Kit formats we ask for. Single source of truth: both the image decoder and the Play
 * Services camera scanner request exactly this set, so a format can never be supported on one
 * path and silently missing on the other.
 */
internal val SUPPORTED_MLKIT_FORMATS: IntArray = intArrayOf(
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

/**
 * Scans a picked image for barcodes using the bundled ML Kit model (no network required).
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
            .setBarcodeFormats(SUPPORTED_MLKIT_FORMATS.first(), *SUPPORTED_MLKIT_FORMATS.drop(1).toIntArray())
            .build()

        val scanner = BarcodeScanning.getClient(options)
        val mlkitBarcodes = try {
            Tasks.await(scanner.process(image))
        } finally {
            scanner.close()
        }

        val codes = mlkitBarcodes.mapNotNull { it.toScannedCode() }

        when (val picked = BarcodeScanPolicy.pick(codes, preferred)) {
            null -> BarcodeScanResult.Failed(UiText.res(R.string.scan_err_no_barcode))
            else -> BarcodeScanResult.Found(picked)
        }
    }.getOrElse { cause ->
        // Only two real failure modes here: the image could not be decoded, or ML Kit itself
        // failed. Both are reported with the underlying cause rather than a generic message.
        val reason = cause.message ?: cause.javaClass.simpleName
        BarcodeScanResult.Failed(UiText.res(R.string.scan_err_read_image, reason))
    }
}

/**
 * Opens the camera scanner that ships with Google Play Services.
 *
 * The scanning UI runs inside Play Services, so this needs no CAMERA permission — which is why
 * it is the only camera path this app has (the spec forbids the permission outright).
 *
 * Unlike [scanImage] this is interactive, so it must never block the calling thread: the scanner
 * needs the main thread to show its activity, and waiting on the task from the main thread would
 * deadlock the UI. Listeners are used instead.
 *
 * Never throws. Returns [BarcodeScanResult.Cancelled] when the user backs out, and a [Failed]
 * with the real reason when Play Services is missing, the scanner module cannot be installed, or
 * the user declines the module download.
 */
suspend fun scanWithPlayServices(
    context: Context,
    preferred: BarcodeFormat,
): BarcodeScanResult {
    val activity = context.findActivity()
        ?: return BarcodeScanResult.Failed(UiText.res(R.string.scan_err_needs_window))

    return suspendCancellableCoroutine { continuation ->
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(SUPPORTED_MLKIT_FORMATS.first(), *SUPPORTED_MLKIT_FORMATS.drop(1).toIntArray())
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(activity, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                if (!continuation.isActive) return@addOnSuccessListener
                val code = barcode.toScannedCode()
                continuation.resume(
                    if (code == null) {
                        BarcodeScanResult.Failed(UiText.res(R.string.scan_err_unsupported))
                    } else {
                        BarcodeScanResult.Found(code)
                    },
                )
            }
            .addOnFailureListener { cause ->
                if (!continuation.isActive) return@addOnFailureListener
                // Backing out of the scanner is a failed task, not an error worth showing.
                val dismissed = cause is ApiException && cause.statusCode == CommonStatusCodes.CANCELED
                continuation.resume(
                    if (dismissed) {
                        BarcodeScanResult.Cancelled
                    } else {
                        // Typical causes: Play Services missing or too old, the scanner module
                        // could not be installed, or the user declined the download.
                        val reason = cause.message ?: cause.javaClass.simpleName
                        BarcodeScanResult.Failed(UiText.res(R.string.scan_err_unavailable, reason))
                    },
                )
            }
    }
}

/** Converts an ML Kit barcode into our model, or null when it is a format we do not support. */
private fun Barcode.toScannedCode(): ScannedCode? {
    val content = rawValue ?: return null
    val format = format.toAppFormat() ?: return null
    return ScannedCode(content, format)
}

/** Unwraps [ContextWrapper]s (Compose hands out themed wrappers) to reach the hosting activity. */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Maps an ML Kit [Barcode.FORMAT_*] constant to our app's [BarcodeFormat].
 * Returns null for formats we do not support (they should be skipped).
 */
internal fun Int.toAppFormat(): BarcodeFormat? = when (this) {
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
