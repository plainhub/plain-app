package com.ismartcoding.plain.platform

import com.ismartcoding.plain.enums.DeviceType
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiom
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiomPhone
import platform.UIKit.UIUserInterfaceIdiomTV

actual fun getAppVersion(): String {
    val bundle = NSBundle.mainBundle
    val shortVersion = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    val buildNumber = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: ""
    return if (buildNumber.isNotEmpty()) "$shortVersion ($buildNumber)" else shortVersion
}

actual fun getOSVersion(): String {
    val device = UIDevice.currentDevice
    return "${device.systemName()} ${device.systemVersion}"
}

actual fun getDeviceName(): String = UIDevice.currentDevice.name

actual fun getBuildType(): String {
    val bundle = NSBundle.mainBundle
    val identifier = bundle.bundleIdentifier ?: ""
    return if (identifier.contains("fdroid")) "fdroid" else if (identifier.contains("github")) "github" else "google"
}

actual fun getPlatformName(): String = "ios"

actual fun getDeviceType(): DeviceType {
    val idiom = UIDevice.currentDevice.userInterfaceIdiom
    return when (idiom) {
        UIUserInterfaceIdiomPad -> DeviceType.TABLET
        UIUserInterfaceIdiomTV -> DeviceType.TV
        UIUserInterfaceIdiomPhone -> DeviceType.PHONE
        else -> DeviceType.OTHER
    }
}

actual fun getDeviceIP4s(): List<String> = emptyList()

actual fun appDir(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)[0] as String