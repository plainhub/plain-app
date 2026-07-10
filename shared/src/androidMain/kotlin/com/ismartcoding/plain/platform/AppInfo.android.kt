package com.ismartcoding.plain.platform

import android.os.Build
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.buildTypeValue
import com.ismartcoding.plain.lib.helpers.NetworkHelper

actual fun getAppVersion(): String {
    val ctx = appContextValue ?: return ""
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    val versionName = pi.versionName ?: ""
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pi.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        pi.versionCode.toLong()
    }
    return "$versionName ($versionCode)"
}

actual fun getOSVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun getDeviceName(): String = PhoneHelper.getDeviceName(appContextValue!!)

actual fun getBuildType(): String = buildTypeValue.ifEmpty { "android" }

actual fun getPlatformName(): String = "android"

actual fun getDeviceType(): DeviceType = PhoneHelper.getDeviceType(appContextValue!!)

actual fun getDeviceIP4s(): List<String> = NetworkHelper.getDeviceIP4s().toList()