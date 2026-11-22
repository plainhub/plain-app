package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.network.wifiAwareAttached
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.media.getDownloadsDirPath
import com.ismartcoding.plain.platform.network.isWifiAwareSupported
import com.ismartcoding.plain.rust.RustStartupConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.UIKit.UIScreen
import platform.UIKit.UIDevice

internal actual fun createRustStartupConfig(): RustStartupConfig = RustStartupConfig(
    debug = isDebugBuild(),
    cacheDir = cacheDirPath(),
    name = UIDevice.currentDevice.name,
    deviceType = getDeviceType().name,
    buildChannel = AppChannelType.fromString(buildChannel).name,
    downloadsDir = getDownloadsDirPath(),
    osName = UIDevice.currentDevice.systemName,
    osVersion = UIDevice.currentDevice.systemVersion,
    appVersion = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "",
    appBuildNumber = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "",
    wifiAwareSupported = isWifiAwareSupported,
    recentFilesCatalog = false,
    systemMediaCatalog = false,
    supportedAbis = emptyList(),
)

@OptIn(ExperimentalForeignApi::class)
internal actual fun readSystemProperties(names: List<String>): Map<String, JsonElement> {
    val display by lazy {
        val screen = UIScreen.mainScreen
        val bounds = screen.bounds
        val scale = screen.scale
        Triple((CGRectGetWidth(bounds) * scale).toInt(), (CGRectGetHeight(bounds) * scale).toInt(), scale)
    }
    return names.associateWith { name ->
        when (name) {
            "wifiAwareRunning" -> JsonPrimitive(wifiAwareAttached.value)
            "language" -> JsonPrimitive(NSLocale.currentLocale.languageCode ?: "")
            "display.width" -> JsonPrimitive(display.first)
            "display.height" -> JsonPrimitive(display.second)
            "display.density" -> JsonPrimitive(display.third)
            else -> error("Unsupported system property: $name")
        }
    }
}
