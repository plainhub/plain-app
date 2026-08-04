package com.ismartcoding.plain.platform

import android.graphics.ImageFormat
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.ismartcoding.plain.helpers.QrCodeBitmapHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
actual fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    onScanResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val reader = remember { QrCodeScanHelper.createReader() }
    val cameraSelector = remember {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    val preview = remember(mainExecutor) {
        Preview.Builder().build().also {
            it.setSurfaceProvider(mainExecutor) { request ->
                surfaceRequest?.invalidate()
                surfaceRequest = request
            }
        }
    }

    val imageAnalysis = remember(executor, cameraDetecting, reader, mainExecutor) {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(executor, QrAnalyzer(reader, cameraDetecting, mainExecutor, onScanResult))
            }
    }

    LaunchedEffect(Unit) {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
        } catch (e: Exception) {
            LogCat.e(e)
            e.printStackTrace()
        }
    }

    DisposableEffect(cameraProvider, lifecycleOwner, cameraSelector, preview, imageAnalysis) {
        val provider = cameraProvider
        if (provider != null) {
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                LogCat.e(e)
                e.printStackTrace()
            }
        }
        onDispose {
            provider?.unbind(preview, imageAnalysis)
            surfaceRequest?.invalidate()
            surfaceRequest = null
        }
    }

    val request = surfaceRequest
    if (request != null) {
        CameraXViewfinder(
            surfaceRequest = request,
            modifier = Modifier.fillMaxSize(),
            implementationMode = ImplementationMode.EMBEDDED,
        )
    }
}

private class QrAnalyzer(
    private val reader: MultiFormatReader,
    private val cameraDetecting: MutableState<Boolean>,
    private val mainExecutor: java.util.concurrent.Executor,
    private val onScanResult: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val supportedImageFormats = listOf(
        ImageFormat.YUV_420_888,
        ImageFormat.YUV_422_888,
        ImageFormat.YUV_444_888,
    )

    override fun analyze(imageProxy: ImageProxy) {
        if (!cameraDetecting.value) {
            imageProxy.close()
            return
        }
        if (imageProxy.format !in supportedImageFormats || imageProxy.planes.size != 3) {
            imageProxy.close()
            return
        }
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

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        return ByteArray(remaining()).also { get(it) }
    }
}

private fun decode(reader: MultiFormatReader, imageProxy: ImageProxy, data: ByteArray): Result {
    val source = PlanarYUVLuminanceSource(
        data,
        imageProxy.planes[0].rowStride,
        imageProxy.height,
        0,
        0,
        imageProxy.width,
        imageProxy.height,
        false,
    )
    return reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
}

actual suspend fun decodeQrFromUri(uri: String): String? {
    return try {
        val context = com.ismartcoding.plain.appContext
        val img = QrCodeBitmapHelper.getBitmapFromUri(context, android.net.Uri.parse(uri))
        QrCodeScanHelper.tryDecode(img)?.text
    } catch (e: Exception) {
        null
    }
}
