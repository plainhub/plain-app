package com.ismartcoding.plain.ui.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap

/** A machine-readable code with its center normalized to the image or camera view. */
data class ScannedCode(val text: String, val centerX: Float, val centerY: Float)

/** A frozen camera frame and the codes detected in it. */
class ScannedFrame(val bitmap: ImageBitmap, val codes: List<ScannedCode>)

/** Codes decoded from a picked image, with positions normalized to its dimensions. */
class ScannedImage(val codes: List<ScannedCode>, val width: Int, val height: Int)

/**
 * Camera preview with real-time multi-code scanning. While [cameraDetecting] is true,
 * each analyzed frame reports its decoded codes and updates [freezeFrame].
 */
@Composable
expect fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    freezeFrame: MutableState<ScannedFrame?>,
    onScanResult: (List<ScannedCode>) -> Unit,
)

/** Creates a decoder that can read image-picker URIs on the current platform. */
@Composable
expect fun rememberQrImageDecoder(): suspend (String) -> ScannedImage?
