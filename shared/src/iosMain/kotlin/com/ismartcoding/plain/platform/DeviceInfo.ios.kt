package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DDisplayInfo
import com.ismartcoding.plain.data.DevicePlatform
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.UIKit.UIScreen
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
actual fun getDeviceInfo(): DDeviceInfo = DDeviceInfo().apply {
    name = UIDevice.currentDevice.name
    platform = DevicePlatform.IOS
    manufacturer = "Apple"
    // utsname.machine is the hardware identifier ("iPhone16,1"), more precise
    // than UIDevice.model which is just the device class ("iPhone").
    model = utsMachine().ifEmpty { UIDevice.currentDevice.model }
    osName = UIDevice.currentDevice.systemName
    osVersion = UIDevice.currentDevice.systemVersion
    kernelVersion = utsRelease()
    appVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    appBuildNumber = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""
    language = NSLocale.currentLocale.languageCode ?: ""
    cpuArch = utsMachine()
    // iOS does not expose a CPU model string; null on the GraphQL side.
    cpuModel = ""
    totalMemory = NSProcessInfo.processInfo.physicalMemory.toLong()
    totalStorage = fileSystemTotalSize()

    val screen = UIScreen.mainScreen
    val scale = screen.scale
    val displayInfo = DDisplayInfo()
    displayInfo.width = (CGRectGetWidth(screen.bounds) * scale).toInt()
    displayInfo.height = (CGRectGetHeight(screen.bounds) * scale).toInt()
    displayInfo.density = scale.toString()
    display = displayInfo
}
