package com.ismartcoding.plain.platform

import android.os.Build
import kotlin.invoke
import kotlin.toString

actual fun isAndroidOnly(): Boolean = true

actual fun isIOS(): Boolean = false

actual fun isHuawei(): Boolean {
    return Build.MANUFACTURER == "HUAWEI"
}
