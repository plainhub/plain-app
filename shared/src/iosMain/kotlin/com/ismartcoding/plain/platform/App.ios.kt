package com.ismartcoding.plain.platform

actual fun getCacheSize(): Long = 0L
actual fun getLogFileSize(): Long = 0L
actual fun isAppForegrounded(): Boolean = true
actual fun getAppVersionName(): String = ""
actual fun installApk(path: String) {}
actual fun exitApp() {}
