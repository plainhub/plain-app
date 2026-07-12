package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.AppLogHelper
import java.io.File
import kotlin.system.exitProcess

actual fun getCacheSize(): Long = AppHelper.getCacheSize(appContext)
actual fun getLogFileSize(): Long = AppLogHelper.getFileSize(appContext)
actual fun isAppForegrounded(): Boolean = AppHelper.foregrounded()
actual fun getAppVersionName(): String = com.ismartcoding.plain.getAppVersionName()
actual fun installApk(path: String) = PackageHelper.install(appContext, File(path))
actual fun exitApp() {
    exitProcess(0)
}
