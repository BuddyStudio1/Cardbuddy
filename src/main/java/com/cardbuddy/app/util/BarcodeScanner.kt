package com.cardbuddy.app.util

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

object BarcodeScanner {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
        
    private val scanner = BarcodeScanning.getClient(options)

    suspend fun scanBarcode(bitmap: Bitmap): Pair<String?, Int> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val barcodes = scanner.process(image).await()
            val firstBarcode = barcodes.firstOrNull()
            if (firstBarcode != null) {
                Pair(firstBarcode.getRawValue(), firstBarcode.getFormat())
            } else {
                Pair(null, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, -1)
        }
    }
}
