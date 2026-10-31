package app.softlanding.ui.add

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The barcode scanner is Google's own screen from Play services: it needs no camera permission in this app
 * and reads EAN/UPC on packs, QR codes and the «Честный знак» DataMatrix.
 */
object Scanner {
    fun scan(ctx: Context, onCode: (String) -> Unit, onFail: (Exception) -> Unit) {
        val opts = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_CODE_128,
            )
            .enableAutoZoom()
            .build()
        runCatching {
            GmsBarcodeScanning.getClient(ctx, opts).startScan()
                .addOnSuccessListener { b -> b.rawValue?.let(onCode) }
                .addOnFailureListener(onFail)
        }.onFailure { onFail(it as? Exception ?: Exception(it)) }
    }

    /** Where the phone's camera writes a photo of a label. */
    fun shotUri(ctx: Context): Uri {
        val dir = File(ctx.cacheDir, "shots").apply { mkdirs() }
        val f = File(dir, "label.jpg")
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)
    }

    /** Text read off a photo, on the phone. */
    suspend fun text(ctx: Context, uri: Uri): String = suspendCancellableCoroutine { cont ->
        val img = InputImage.fromFilePath(ctx, uri)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img)
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}
