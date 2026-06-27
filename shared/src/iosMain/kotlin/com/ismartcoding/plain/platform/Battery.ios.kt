package com.ismartcoding.plain.platform

import platform.UIKit.UIDevice

actual fun getBatteryLevel(): Int {
    UIDevice.currentDevice.batteryMonitoringEnabled = true
    val level = UIDevice.currentDevice.batteryLevel
    return if (level < 0) -1 else (level * 100).toInt()
}