package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DevicePlatform
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.UIKit.UIDevice

actual fun getDeviceInfo(): DDeviceInfo = DDeviceInfo().apply {
    name = UIDevice.currentDevice.name
    platform = DevicePlatform.IOS
    manufacturer = "Apple"
    model = UIDevice.currentDevice.model
    osName = UIDevice.currentDevice.systemName()
    osVersion = UIDevice.currentDevice.systemVersion
    appVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    appBuildNumber = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""
    language = NSLocale.currentLocale.languageCode ?: ""
    uptime = NSProcessInfo.processInfo.systemUptime.toLong() * 1000L
}
