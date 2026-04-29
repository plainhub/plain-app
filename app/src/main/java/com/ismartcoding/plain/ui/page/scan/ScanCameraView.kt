package com.ismartcoding.plain.ui.page.scan

import android.graphics.ImageFormat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
internal fun ScanCameraView(
    lifecycleOwner: LifecycleOwner,
    cameraDetecting: MutableState<Boolean>,
    onCameraProvider: (ProcessCameraProvider) -> Unit,
    onScanResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val reader = remember { QrCodeScanHelper.createReader() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val preview = Preview.Builder().build()
        val selector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preview.surfaceProvider = previewView.surfaceProvider
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(executor, object : ImageAnalysis.Analyzer {
            private val supportedImageFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
            override fun analyze(imageProxy: ImageProxy) {
                if (!cameraDetecting.value) { imageProxy.close(); return }
                if (imageProxy.format !in supportedImageFormats || imageProxy.planes.size != 3) { imageProxy.close(); return }
                val data = imageProxy.planes[0].buffer.toByteArray()
                try {
                    cameraDetecting.value = false
                    try {
                        val text = decode(reader, imageProxy, data).text
                        mainExecutor.execute { onScanResult(text) }
                    } catch (e: NotFoundException) {
                        for (i in data.indices) data[i] = (255 - (data[i].toInt() and 0xff)).toByte()
                        val text = decode(reader, imageProxy, data).text
                        mainExecutor.execute { onScanResult(text) }
                    }
                } catch (e: Exception) {
                    cameraDetecting.value = true
                    e.printStackTrace()
                } finally {
                    imageProxy.close()
                }
            }
            private fun ByteBuffer.toByteArray(): ByteArray { rewind(); return ByteArray(remaining()).also { get(it) } }
        })
        try {
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                try {
                    val provider = future.get(); onCameraProvider(provider)
                    provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (e: Exception) { LogCat.e(e); e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
        } catch (e: Exception) { LogCat.e(e); e.printStackTrace() }
        previewView
    }, modifier = Modifier.fillMaxSize())
}

internal fun decode(reader: MultiFormatReader, imageProxy: ImageProxy, data: ByteArray): Result {
    val source = PlanarYUVLuminanceSource(data, imageProxy.planes[0].rowStride, imageProxy.height, 0, 0, imageProxy.width, imageProxy.height, false)
    return reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
}

