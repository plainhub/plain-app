package com.ismartcoding.plain.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ismartcoding.plain.appContextValue

actual fun getBatteryLevel(): Int {
    val ctx = appContextValue ?: return -1
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return if (level in 0..100) level else -1
}