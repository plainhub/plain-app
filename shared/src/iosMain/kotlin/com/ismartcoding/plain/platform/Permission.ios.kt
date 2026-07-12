package com.ismartcoding.plain.platform

actual fun isPermissionGranted(perm: String): Boolean = true

actual fun Permission.isGranted(): Boolean = when (this) {
    // iOS permission checks are async-only (UNUserNotificationCenter, AVCaptureDevice, etc.)
    // Return true here; the request flow handles the real check.
    else -> true
}
