package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DBattery
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState

actual fun getBatteryLevel(): Int {
    UIDevice.currentDevice.batteryMonitoringEnabled = true
    val level = UIDevice.currentDevice.batteryLevel
    return if (level < 0) -1 else (level * 100).toInt()
}

actual fun getBattery(): DBattery = DBattery().apply {
    level = getBatteryLevel()
    status = when (UIDevice.currentDevice.batteryState) {
        UIDeviceBatteryState.UIDeviceBatteryStateUnknown -> 0
        UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> 1
        UIDeviceBatteryState.UIDeviceBatteryStateCharging -> 2
        UIDeviceBatteryState.UIDeviceBatteryStateFull -> 3
    }
}

actual fun isUsbConnected(): Boolean = false
