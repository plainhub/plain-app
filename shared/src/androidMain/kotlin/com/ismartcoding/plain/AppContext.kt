package com.ismartcoding.plain

import android.content.Context
import android.os.Build

@PublishedApi
internal var appContextValue: Context? = null

fun setAppContext(context: Context) {
    appContextValue = context
}

val appContext: Context
    get() = appContextValue ?: error("setAppContext must be called before appContext is used")

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

fun getAndroidVersion(): String {
    return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
