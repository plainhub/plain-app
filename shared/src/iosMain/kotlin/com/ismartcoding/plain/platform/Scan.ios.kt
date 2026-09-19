@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.Foundation.NSCoder
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

private val SCAN_METADATA_TYPES = listOf(
    AVMetadataObjectTypeQRCode,
    AVMetadataObjectTypeAztecCode,
    AVMetadataObjectTypeCode128Code,
    AVMetadataObjectTypeCode39Code,
    AVMetadataObjectTypeEAN13Code,
    AVMetadataObjectTypeEAN8Code,
    AVMetadataObjectTypeUPCECode,
    AVMetadataObjectTypeITF14Code,
)

/** Hands the preview layer to the metadata delegate; both live on the main thread. */
private class PreviewLayerHolder {
    var layer: AVCaptureVideoPreviewLayer? = null
}

@Composable
actual fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    freezeFrame: MutableState<ScannedFrame?>,
    onScanResult: (List<ScannedCode>) -> Unit,
) {
    val session = remember { AVCaptureSession() }
    val layerHolder = remember { PreviewLayerHolder() }
    val delegate = remember { ScanMetadataDelegate(cameraDetecting, layerHolder, onScanResult) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            session.beginConfiguration()
            session.sessionPreset = AVCaptureSessionPresetHigh
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                memScoped {
                    val errorPtr: ObjCObjectVar<NSError?> = alloc<ObjCObjectVar<NSError?>>()
                    val input = AVCaptureDeviceInput(device, errorPtr.ptr)
                    if (input != null && session.canAddInput(input)) {
                        session.addInput(input)
                    }
                }
            }
            val metadataOutput = AVCaptureMetadataOutput()
            if (session.canAddOutput(metadataOutput)) {
                session.addOutput(metadataOutput)
                metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
                metadataOutput.metadataObjectTypes = SCAN_METADATA_TYPES
            }
            session.commitConfiguration()
            session.startRunning()
        }
    }

    // freeze = stop the session; the preview layer holds the last frame until resumed
    LaunchedEffect(cameraDetecting.value) {
        withContext(Dispatchers.Default) {
            if (cameraDetecting.value) {
                if (!session.isRunning()) session.startRunning()
            } else {
                if (session.isRunning()) session.stopRunning()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            session.stopRunning()
        }
    }

    UIKitView(
        factory = {
            CameraPreviewContainerView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                attachSession(session)
                layerHolder.layer = previewLayer
                autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.attachSession(session)
            layerHolder.layer = view.previewLayer
        },
    )
}

internal class CameraPreviewContainerView : UIView {
    var previewLayer: AVCaptureVideoPreviewLayer? = null
        private set

    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    fun attachSession(session: AVCaptureSession) {
        if (previewLayer != null) return
        val layer = AVCaptureVideoPreviewLayer.layerWithSession(session)
        layer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.setFrame(bounds)
        this.layer.addSublayer(layer)
        previewLayer = layer
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.setFrame(bounds)
    }
}

private class ScanMetadataDelegate(
    private val cameraDetecting: MutableState<Boolean>,
    private val layerHolder: PreviewLayerHolder,
    private val onScanResult: (List<ScannedCode>) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection,
    ) {
        if (!cameraDetecting.value) return
        val layer = layerHolder.layer ?: return
        val layerSize = layer.bounds.useContents { size }
        if (layerSize.width <= 0.0 || layerSize.height <= 0.0) return
        val codes = ArrayList<ScannedCode>(didOutputMetadataObjects.size)
        for (obj in didOutputMetadataObjects) {
            val metadata = obj as? AVMetadataMachineReadableCodeObject ?: continue
            val text = metadata.stringValue ?: continue
            val bounds = (layer.transformedMetadataObjectForMetadataObject(metadata) as? AVMetadataMachineReadableCodeObject)
                ?.bounds
                ?.useContents { Pair(origin.x + size.width / 2.0, origin.y + size.height / 2.0) }
                ?: continue
            val centerX = (bounds.first / layerSize.width).toFloat().coerceIn(0f, 1f)
            val centerY = (bounds.second / layerSize.height).toFloat().coerceIn(0f, 1f)
            if (codes.none { it.text == text }) {
                codes.add(ScannedCode(text, centerX, centerY))
            }
        }
        onScanResult(codes)
    }
}

actual suspend fun decodeQrFromUri(uri: String): ScannedImage? {
    return try {
        withContext(Dispatchers.Default) {
            val nsUrl: NSURL = NSURL.URLWithString(uri) ?: return@withContext null
            val path: String = nsUrl.path ?: return@withContext null
            val data: platform.Foundation.NSData =
                NSFileManager.defaultManager.contentsAtPath(path) ?: return@withContext null
            val image: UIImage = UIImage.imageWithData(data) ?: return@withContext null
            // file-backed UIImages have no CIImage; bridge through CGImage instead
            val ciImage = image.CIImage()
                ?: image.CGImage?.let { CIImage.imageWithCGImage(it) }
                ?: return@withContext null
            val context = CIContext.context()
            val detector: CIDetector = CIDetector.detectorOfType(
                CIDetectorTypeQRCode, context, null,
            ) ?: return@withContext null
            val features: List<*> = detector.featuresInImage(ciImage) ?: return@withContext null
            val extent = ciImage.extent()
            val extentWidth = extent.useContents { size.width }
            val extentHeight = extent.useContents { size.height }
            if (extentWidth <= 0.0 || extentHeight <= 0.0) return@withContext null
            val codes = ArrayList<ScannedCode>(features.size)
            for (raw in features) {
                val feature = raw as? CIQRCodeFeature ?: continue
                val msg = feature.messageString ?: continue
                if (msg.isEmpty()) continue
                val bounds = feature.bounds.useContents { Pair(origin.x + size.width / 2.0, origin.y + size.height / 2.0) }
                val centerX = (bounds.first / extentWidth).toFloat().coerceIn(0f, 1f)
                val centerY = (bounds.second / extentHeight).toFloat().coerceIn(0f, 1f)
                if (codes.none { it.text == msg }) {
                    codes.add(ScannedCode(msg, centerX, centerY))
                }
            }
            if (codes.isEmpty()) null else ScannedImage(codes, extentWidth.toInt(), extentHeight.toInt())
        }
    } catch (e: Exception) {
        null
    }
}
