package com.ismartcoding.plain.platform

import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import com.google.zxing.MultiFormatReader
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.QrCodeBitmapHelper
import com.ismartcoding.plain.helpers.QrCodeScanHelper
import com.ismartcoding.plain.helpers.QrScanPipeline
import com.ismartcoding.plain.helpers.ScanCoordMapper
import com.ismartcoding.plain.helpers.YPlane
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
actual fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    freezeFrame: MutableState<ScannedFrame?>,
    onScanResult: (List<ScannedCode>) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val reader = remember { QrScanPipeline.createReader() }
    val cameraSelector = remember {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

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

    val imageAnalysis = remember(executor, cameraDetecting, reader, mainExecutor, freezeFrame) {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                    )
                    .build()
            )
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    executor,
                    ScanFrameAnalyzer(reader, cameraDetecting, freezeFrame, mainExecutor, onScanResult) { viewSize }
                )
            }
    }

    LaunchedEffect(Unit) {
        try {
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
        } catch (e: Exception) {
            LogCat.e(e)
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
            }
        }
        onDispose {
            provider?.unbind(preview, imageAnalysis)
            surfaceRequest?.invalidate()
            surfaceRequest = null
        }
    }

    val request = surfaceRequest
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
    ) {
        if (request != null) {
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize(),
                implementationMode = ImplementationMode.EMBEDDED,
            )
        }
    }
}

/**
 * Decodes every code in each analyzed frame (throttled to [FRAME_INTERVAL_MS]) on a
 * dedicated executor thread, mapping decoded positions to view-normalized coordinates.
 * Frames that decode at least one code are also converted to an upright bitmap
 * snapshot in [freezeFrame] so the page can freeze the viewfinder on them.
 */
private class ScanFrameAnalyzer(
    private val reader: MultiFormatReader,
    private val cameraDetecting: MutableState<Boolean>,
    private val freezeFrame: MutableState<ScannedFrame?>,
    private val mainExecutor: Executor,
    private val onScanResult: (List<ScannedCode>) -> Unit,
    private val viewSize: () -> IntSize,
) : ImageAnalysis.Analyzer {
    private val supportedImageFormats = listOf(
        ImageFormat.YUV_420_888,
        ImageFormat.YUV_422_888,
        ImageFormat.YUV_444_888,
    )

    private val packed = YPlane()
    private val upright = YPlane()
    private var lastAnalyzedNs = 0L

    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (!cameraDetecting.value) return
            if (imageProxy.format !in supportedImageFormats || imageProxy.planes.size != 3) return
            val now = System.nanoTime()
            if (now - lastAnalyzedNs < FRAME_INTERVAL_MS * 1_000_000L) return
            lastAnalyzedNs = now

            val yPlane = imageProxy.planes[0]
            QrScanPipeline.copyPackedYPlane(yPlane.buffer, imageProxy.width, imageProxy.height, yPlane.rowStride, packed)
            val frame = QrScanPipeline.rotate(packed, imageProxy.imageInfo.rotationDegrees, upright)
            val codes = QrScanPipeline.decode(reader, frame.data, frame.width, frame.height)
            if (codes.isEmpty()) return
            val view = viewSize()
            val mapped = ArrayList<ScannedCode>(codes.size)
            for (code in codes) {
                val (nx, ny) = ScanCoordMapper.toViewNormalized(
                    code.centerX, code.centerY, frame.width, frame.height, view.width, view.height
                )
                mapped.add(ScannedCode(code.text, nx, ny))
            }
            // the freeze snapshot is a display nicety: if it fails on exotic plane
            // layouts the detection callback below must still fire
            var snapshot: ImageBitmap? = null
            try {
                snapshot = snapshotBitmap(imageProxy)
            } catch (e: Exception) {
                LogCat.e(e)
            }
            freezeFrame.value = snapshot?.let { ScannedFrame(it, mapped) }
            mainExecutor.execute { onScanResult(mapped) }
        } catch (e: Exception) {
            LogCat.e(e)
        } finally {
            imageProxy.close()
        }
    }

    /** YUV frame → NV21 → JPEG → upright bitmap, matching the viewfinder orientation. */
    private fun snapshotBitmap(image: ImageProxy): ImageBitmap {
        val nv21 = QrScanPipeline.buildNv21(
            image.planes[0].buffer, image.planes[0].rowStride,
            image.planes[1].buffer, image.planes[1].rowStride, image.planes[1].pixelStride,
            image.planes[2].buffer, image.planes[2].rowStride, image.planes[2].pixelStride,
            image.width, image.height,
        )
        val yuv = android.graphics.YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val jpeg = java.io.ByteArrayOutputStream()
        yuv.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 85, jpeg)
        var bitmap = android.graphics.BitmapFactory.decodeByteArray(jpeg.toByteArray(), 0, jpeg.size())
            ?: throw IllegalStateException("snapshot JPEG decode failed")
        val degrees = image.imageInfo.rotationDegrees
        if (degrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(degrees.toFloat())
            bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        }
        return bitmap.asImageBitmap()
    }

    companion object {
        const val FRAME_INTERVAL_MS = 100L
    }
}

actual suspend fun decodeQrFromUri(uri: String): ScannedImage? {
    return try {
        withContext(Dispatchers.Default) {
            val img = QrCodeBitmapHelper.getBitmapFromUri(appContext, android.net.Uri.parse(uri))
            QrCodeScanHelper.decodeAll(img)
        }
    } catch (e: Exception) {
        null
    }
}
