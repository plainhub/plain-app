package com.ismartcoding.plain.platform

import android.os.Build
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.buildTypeValue
import com.ismartcoding.plain.lib.extensions.appDir as contextAppDir
import com.ismartcoding.plain.lib.helpers.NetworkHelper

actual fun getAppVersion(): String {
    val ctx = appContextValue ?: return ""
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    val versionName = pi.versionName ?: ""
    val versionCode = pi.longVersionCode
    return "$versionName ($versionCode)"
}

actual fun getOSVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun getDeviceName(): String = PhoneHelper.getDeviceName(appContextValue!!)

actual fun getBuildType(): String = buildTypeValue.ifEmpty { "android" }

actual fun getPlatformName(): String = "android"

actual fun getDeviceType(): DeviceType = PhoneHelper.getDeviceType(appContextValue!!)

actual fun getDeviceIP4s(): List<String> = NetworkHelper.getDeviceIP4s().toList()

actual fun appDir(): String = appContext.contextAppDir()

actual fun databaseFilePath(name: String): String =
    appContext.getDatabasePath(name).absolutePath

actual fun getOwnPackageName(): String = appContext.packageName

actual fun dataStoreFilePath(): String =
    appContext.filesDir.absolutePath + "/datastore/settings.preferences_pb"

actual fun getAppVersionCode(): Long = com.ismartcoding.plain.getAppVersionCode()

actual fun isDebugBuild(): Boolean = com.ismartcoding.plain.isDebugBuild()

actual fun getSdkInt(): Int = android.os.Build.VERSION.SDK_INT