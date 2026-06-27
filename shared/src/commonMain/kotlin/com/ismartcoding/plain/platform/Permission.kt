package com.ismartcoding.plain.platform

/**
 * Check whether a runtime permission is granted without prompting the user.
 *
 * @param perm Android: `android.Manifest.permission.*` (e.g. "android.permission.CAMERA").
 *             iOS: empty string (returns true on iOS, all permissions queried via OS APIs).
 */
expect fun isPermissionGranted(perm: String): Boolean