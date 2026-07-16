package com.ismartcoding.plain.platform

import org.jetbrains.compose.resources.StringResource

actual fun isPermissionGranted(perm: String): Boolean = true

actual fun Permission.isGranted(): Boolean = when (this) {
    // iOS permission checks are async-only (UNUserNotificationCenter, AVCaptureDevice, etc.)
    // Return true here; the request flow handles the real check.
    else -> true
}

actual suspend fun ensureNotificationPermissionAsync(): Boolean = true

actual fun checkNotificationPermission(stringResource: StringResource, onGranted: () -> Unit) {
    // iOS has no equivalent runtime permission dialog flow that blocks here.
    // The first UNUserNotificationRequest triggers the system prompt, so just
    // proceed with the callback.
    onGranted()
}
