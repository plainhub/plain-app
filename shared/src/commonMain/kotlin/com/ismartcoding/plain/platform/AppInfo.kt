package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.DeviceType

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

/**
 * Platform name for protocol headers: "android" or "ios".
 */
expect fun getPlatformName(): String

/**
 * Device type for discovery/pairing protocol.
 */
expect fun getDeviceType(): DeviceType

/**
 * All IPv4 addresses of the device. Empty list on iOS (BLE-only).
 */
expect fun getDeviceIP4s(): List<String>