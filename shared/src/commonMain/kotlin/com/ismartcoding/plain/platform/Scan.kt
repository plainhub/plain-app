package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.ImageBitmap

/**
 * A machine-readable code detected by the scanner.
 *
 * @param text decoded content of the code.
 * @param centerX normalized horizontal center of the code within the visible camera
 *   area, 0..1, in display orientation (portrait-up).
 * @param centerY normalized vertical center of the code within the visible camera area, 0..1.
 */
data class ScannedCode(val text: String, val centerX: Float, val centerY: Float)

/**
 * A frozen camera frame that contained detected codes. The bitmap is upright
 * (display orientation) and its codes use view-normalized coordinates matching an
 * aspect-fill render of the bitmap.
 */
class ScannedFrame(val bitmap: ImageBitmap, val codes: List<ScannedCode>)

/**
 * Codes decoded from a picked image, with the image dimensions the positions are
 * normalized against.
 */
class ScannedImage(val codes: List<ScannedCode>, val width: Int, val height: Int)

/**
 * Camera preview with real-time multi-code scanning. Invokes [onScanResult] on the
 * main thread once per analyzed frame with every code currently decoded (empty
 * list when none). While [cameraDetecting] is true every frame that decodes at
 * least one code also updates [freezeFrame] with the upright frame snapshot.
 *
 * @param cameraDetecting mutable flag; while false the analyzer drops frames without
 *   decoding (camera keeps running).
 * @param freezeFrame written by the implementation with the latest detection frame;
 *   the caller clears it to resume live preview.
 */
@Composable
expect fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    freezeFrame: MutableState<ScannedFrame?>,
    onScanResult: (List<ScannedCode>) -> Unit,
)

/**
 * Decode codes from an image picked via the system image picker.
 *
 * @param uri the content URI string returned by the picker (e.g. "content://...").
 * @return the codes found with positions normalized to the image, or null if none.
 */
expect suspend fun decodeQrFromUri(uri: String): ScannedImage?
