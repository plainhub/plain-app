@file:JvmName("AppContextLib")

package com.ismartcoding.plain

import android.content.Context
import android.content.pm.ApplicationInfo

@PublishedApi
internal var appContextValue: Context? = null

val appContextOrNull: Context?
    get() = appContextValue

val appContext: Context
    get() = appContextValue
        ?: error("setAppContext must be called before appContext is used")

fun setAppContext(context: Context, buildType: String = "", buildChannel: String = "") {
    appContextValue = context.applicationContext
    buildTypeValue = buildType
    buildChannelValue = buildChannel
}

fun isDebugBuild(): Boolean {
    val ctx = appContextOrNull ?: return false
    return (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

fun getAppVersion(): String {
    val ctx = appContextOrNull ?: return ""
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    val versionName = pi.versionName ?: ""
    val versionCode = pi.longVersionCode
    return "$versionName ($versionCode)"
}

fun getAppVersionName(): String {
    val ctx = appContextOrNull ?: return ""
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    return pi.versionName ?: ""
}

fun getAppVersionCode(): Long {
    val ctx = appContextOrNull ?: return 0L
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    return pi.longVersionCode
}