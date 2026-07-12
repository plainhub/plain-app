package com.ismartcoding.plain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

@PublishedApi
internal var appContextValue: Context? = null

@PublishedApi
internal var buildTypeValue: String = ""

var mainActivity: AppCompatActivity? = null

fun setAppContext(context: Context, buildType: String = "", buildChannel: String = "") {
    appContextValue = context
    buildTypeValue = buildType
    buildChannelValue = buildChannel
}

val appContext: Context
    get() = appContextValue ?: error("setAppContext must be called before appContext is used")

fun isDebugBuild(): Boolean {
    val ctx = appContextValue ?: return false
    return (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

fun getAppVersion(): String {
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

fun getAppVersionName(): String {
    val ctx = appContextValue ?: return ""
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    return pi.versionName ?: ""
}

fun getAppVersionCode(): Long {
    val ctx = appContextValue ?: return 0L
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pi.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        pi.versionCode.toLong()
    }
}

fun getAndroidVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
