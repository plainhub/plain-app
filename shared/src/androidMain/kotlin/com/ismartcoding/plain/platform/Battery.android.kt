package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.appContextValue
import com.ismartcoding.plain.data.DBattery
import com.ismartcoding.plain.receivers.BatteryReceiver
import com.ismartcoding.plain.receivers.PlugInControlReceiver

actual fun getBatteryLevel(): Int {
    val ctx = appContextValue ?: return -1
    val bm = ctx.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager ?: return -1
    val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return if (level in 0..100) level else -1
}

actual fun getBattery(): DBattery = BatteryReceiver.get(appContext)

actual fun isUsbConnected(): Boolean = PlugInControlReceiver.isUSBConnected(appContext)
