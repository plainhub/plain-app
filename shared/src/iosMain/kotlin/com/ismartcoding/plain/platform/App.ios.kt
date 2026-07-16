package com.ismartcoding.plain.platform

actual fun getCacheSize(): Long = 0L
actual fun getLogFileSize(): Long = 0L
actual fun isAppForegrounded(): Boolean = true
actual fun getAppVersionName(): String = ""
actual fun installApk(path: String) {}
actual fun exitApp() {}

actual fun clearCacheAsync() {}
actual fun clearLogsAsync() {}
actual fun exportLogsAsync() {}
actual fun clearImageMemoryCache() {}
actual suspend fun checkUpdateAsync(showToast: Boolean): Boolean? = null
actual fun backup(uriStr: String) {}
actual fun restore(uriStr: String) {}
actual fun backupToFile(fileName: String) {}

actual fun getAppIcon(packageName: String): Any? = null
actual fun isIgnoringBatteryOptimizations(): Boolean = false
actual fun openBatteryOptimizationSettings() {}
