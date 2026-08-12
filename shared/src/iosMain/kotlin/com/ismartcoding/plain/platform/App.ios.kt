@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.timeIntervalSince1970

actual fun getCacheSize(): Long = 0L

actual fun getLogFileSize(): Long {
    val filePath = getLatestLogFilePath()
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(filePath)) return 0L
    val attrs = fm.attributesOfItemAtPath(filePath, null)
    return (attrs?.get(NSFileSize) as? Long) ?: 0L
}

actual fun isAppForegrounded(): Boolean = true
actual fun getAppVersionName(): String = ""
actual fun installApk(path: String) {}
actual fun exitApp() {}

actual fun clearCacheAsync() {}

actual fun clearLogsAsync() {
    clearLatestLogFile()
    val backupPath = getLatestLogFilePath() + ".bak"
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(backupPath)) {
        fm.removeItemAtPath(backupPath, null)
    }
}

actual fun exportLogsAsync() {
    val srcPath = getLatestLogFilePath()
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(srcPath)) return
    val destPath = appDir() + "/logs_export_${(NSDate().timeIntervalSince1970 * 1000).toLong()}.log"
    if (fm.fileExistsAtPath(destPath)) {
        fm.removeItemAtPath(destPath, null)
    }
    fm.copyItemAtPath(srcPath, destPath, null)
}

actual fun clearImageMemoryCache() {}
actual suspend fun checkUpdateAsync(showToast: Boolean): Boolean? = null
actual fun backup(uriStr: String) {}
actual fun restore(uriStr: String) {}
actual fun backupToFile(fileName: String) {}

actual fun getAppIcon(packageName: String): Any? = null
actual fun isIgnoringBatteryOptimizations(): Boolean = false
actual fun openBatteryOptimizationSettings() {}