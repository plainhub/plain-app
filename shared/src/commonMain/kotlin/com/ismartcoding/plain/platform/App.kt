package com.ismartcoding.plain.platform

expect fun getCacheSize(): Long
expect fun getLogFileSize(): Long
expect fun isAppForegrounded(): Boolean

/** App version name (e.g. "1.0.0"), without the version code suffix. */
expect fun getAppVersionName(): String

/**
 * Install an APK file at [path] via the system package installer.
 * No-op on platforms without a package installer (iOS).
 */
expect fun installApk(path: String)

/** Terminate the app process immediately. */
expect fun exitApp()

/** Clear app cache directories (cacheDir, image_cache, upload_tmp). */
expect fun clearCacheAsync()

/** Clear all log files. */
expect fun clearLogsAsync()

/** Export logs to a shareable zip file. */
expect fun exportLogsAsync()

/** Clear in-memory image cache (Coil). */
expect fun clearImageMemoryCache()

/**
 * Check for app updates from the release source.
 * @param showToast whether to show a toast on rate-limit / failure
 * @return true if an update is available, false if up-to-date, null on failure
 */
expect suspend fun checkUpdateAsync(showToast: Boolean): Boolean?

/**
 * Backup app data to a file chosen via the system file picker.
 * @param uriStr URI string returned by the file picker (Android SAF or equivalent)
 */
expect fun backup(uriStr: String)

/**
 * Restore app data from a backup zip picked via the system file picker.
 * @param uriStr URI string returned by the file picker
 */
expect fun restore(uriStr: String)

/**
 * Backup app data directly to app-specific external storage (Downloads).
 * Used on Android 9 and below where ACTION_CREATE_DOCUMENT is unreliable.
 * @param fileName destination file name
 */
expect fun backupToFile(fileName: String)

/**
 * Load the icon for the app with [packageName].
 * Returns a platform-specific model suitable for use with coil3 `AsyncImage`
 * (Android: `android.graphics.drawable.Drawable`; iOS: `UIImage`).
 * Returns null if the app is not installed or the icon is unavailable.
 */
expect fun getAppIcon(packageName: String): Any?

/**
 * Whether the app is currently exempt from battery optimizations.
 * Always false on iOS (no equivalent concept).
 */
expect fun isIgnoringBatteryOptimizations(): Boolean

/**
 * Open the platform's battery-optimization settings screen so the user can
 * manage exemptions. No-op on iOS.
 */
expect fun openBatteryOptimizationSettings()
