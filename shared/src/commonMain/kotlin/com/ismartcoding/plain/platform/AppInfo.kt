package com.ismartcoding.plain.platform

/**
 * App version as displayed to the user (e.g. "1.0.0 (100)").
 */
expect fun getAppVersion(): String

/**
 * OS version string.
 * Android: "Android 14 (API 34)"
 * iOS: "iOS 17.5"
 */
expect fun getOSVersion(): String

/**
 * Human-readable device name.
 * Android: manufacturer + model (e.g. "Google Pixel 7")
 * iOS: UIDevice.current.name
 */
expect fun getDeviceName(): String

/**
 * Build flavor + variant identifier (e.g. "fdroid-debug", "google-release").
 */
expect fun getBuildType(): String