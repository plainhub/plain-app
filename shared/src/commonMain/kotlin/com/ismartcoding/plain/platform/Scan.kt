package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

/**
 * Camera preview with real-time QR code scanning.
 *
 * @param cameraDetecting mutable flag; set to false while a decode is in flight
 *   and back to true when ready to scan again.
 * @param onScanResult invoked on the main thread with the decoded QR text.
 */
@Composable
expect fun ScanCameraView(
    cameraDetecting: MutableState<Boolean>,
    onScanResult: (String) -> Unit,
)

/**
 * Decode a QR code from an image picked via the system image picker.
 *
 * @param uri the content URI string returned by the picker (e.g. "content://...").
 * @return the decoded text, or null if no QR code is found or decoding fails.
 */
expect suspend fun decodeQrFromUri(uri: String): String?
