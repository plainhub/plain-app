@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorTypeQRCode
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
import platform.objc.sel_registerName

@Composable
actual fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    onScanResult: (String) -> Unit,
) {
    val session = remember { AVCaptureSession() }
    val delegate = remember { ScanMetadataDelegate(cameraDetecting, onScanResult) }

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
                metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
            }
            session.commitConfiguration()
            session.startRunning()
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
                autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.attachSession(session)
        },
    )
}

internal class CameraPreviewContainerView : UIView {
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    fun attachSession(session: AVCaptureSession) {
        val existing = previewLayer
        if (existing != null) return
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
    private val onScanResult: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        if (!cameraDetecting.value) return
        for (obj in didOutputMetadataObjects) {
            if (obj is AVMetadataMachineReadableCodeObject) {
                val text = obj.stringValue
                if (text != null) {
                    cameraDetecting.value = false
                    onScanResult(text)
                    break
                }
            }
        }
    }
}

actual suspend fun decodeQrFromUri(uri: String): String? {
    return try {
        val nsUrl: NSURL = NSURL.URLWithString(uri) ?: return null
        val path: String = nsUrl.path ?: return null
        val data: platform.Foundation.NSData = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
        val image: UIImage = UIImage.imageWithData(data) ?: return null
        val ciImage: platform.CoreImage.CIImage = image.CIImage() ?: return null
        val context: CIContext = CIContext.context()
        val detector: CIDetector = CIDetector.detectorOfType(
            CIDetectorTypeQRCode, context, null,
        ) ?: return null
        val features: List<*> = detector.featuresInImage(ciImage) ?: return null
        for (raw in features) {
            val feature: NSObject = raw as? NSObject ?: continue
            val msg: String? = feature.performSelector(sel_registerName("messageString")) as? String
            if (msg != null && msg.isNotEmpty()) return msg
        }
        null
    } catch (e: Exception) {
        null
    }
}
