package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
actual fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    onScanResult: (String) -> Unit,
) {
    // iOS camera scanning not yet implemented
}

actual suspend fun decodeQrFromUri(uri: String): String? = null
