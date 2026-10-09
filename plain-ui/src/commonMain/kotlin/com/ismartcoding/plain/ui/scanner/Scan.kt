package com.ismartcoding.plain.ui.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

/**
 * Camera preview with real-time multi-code scanning. While [cameraDetecting] is true,
 * each analyzed frame reports its decoded codes and updates [freezeFrame].
 */
@Composable
expect fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    freezeFrame: MutableState<ScannedFrame?>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onScanResult: (List<ScannedCode>) -> Unit,
)

/** Creates a decoder that can read image-picker URIs on the current platform. */
@Composable
expect fun rememberQrImageDecoder(): suspend (String) -> ScannedImage?
