package org.ubaierbhat.android.barcodekeyboard.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val throttle: ScanThrottle,
    private val onResult: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build(),
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        try {
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val text = barcodes.firstNotNullOfOrNull { it.rawValue }
                    if (text != null && throttle.allow()) {
                        onResult(text)
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } catch (error: RuntimeException) {
            imageProxy.close()
        }
    }

    fun close() {
        scanner.close()
    }
}
