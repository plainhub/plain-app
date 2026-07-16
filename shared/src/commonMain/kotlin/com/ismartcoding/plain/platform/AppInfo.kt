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

/**
 * App's external files directory (Android) or Documents directory (iOS).
 * Used to resolve `app://` and `fid:` paths.
 */
expect fun appDir(): String

/**
 * Absolute path of the database file with the given [name].
 * On Android this is `<filesDir>/databases/<name>` (matching Room's default
 * location so existing databases are preserved across the KMP migration).
 * On iOS this is `<Documents>/<name>`.
 */
expect fun databaseFilePath(name: String): String

/**
 * The app's own package name / bundle identifier.
 */
expect fun getOwnPackageName(): String

/**
 * Absolute path to the preferences DataStore file (settings.preferences_pb).
 * On Android this is `<filesDir>/datastore/settings.preferences_pb`.
 */
expect fun dataStoreFilePath(): String

/**
 * Numeric app version code (e.g. 100). 0 if not available.
 */
expect fun getAppVersionCode(): Long

/**
 * Whether the running app is a debuggable build.
 */
expect fun isDebugBuild(): Boolean

/**
 * Platform-specific SDK/API level integer. On Android this is Build.VERSION.SDK_INT.
 * Returns 0 on iOS (no equivalent notion).
 */
expect fun getSdkInt(): Int