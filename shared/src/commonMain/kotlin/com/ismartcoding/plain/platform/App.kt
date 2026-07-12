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
