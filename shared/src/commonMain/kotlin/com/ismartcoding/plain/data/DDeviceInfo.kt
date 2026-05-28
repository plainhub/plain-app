package com.ismartcoding.plain.data

import kotlin.time.Instant
import com.ismartcoding.plain.helpers.TimeHelper

enum class DevicePlatform {
    ANDROID, IOS, MACOS, WINDOWS, LINUX
}

class DAndroidDeviceInfo {
    var sdkVersion: Int = 0
    var versionCodeName: String = ""
    var securityPatch: String = ""
    var bootloader: String = ""
    var fingerprint: String = ""
    var hardware: String = ""
    var radioVersion: String = ""
    var board: String = ""
    var buildBrand: String = ""
    var buildHost: String = ""
    var buildUser: String = ""
    var buildNumber: String = ""
    var product: String = ""
    var device: String = ""
    var javaVmVersion: String = ""
    var glEsVersion: String = ""
    var serial: String = ""
    var buildTime: Instant = TimeHelper.now()
}

class DDesktopDeviceInfo {
    var hostname: String = ""
    var cpuModel: String = ""
    var gpuModel: String = ""
    var desktopEnvironment: String = ""
    var windowManager: String = ""
}

class DDisplayInfo {
    var width: Int = 0
    var height: Int = 0
    var density: String = ""
}

class DDeviceInfo {
    var name: String = ""
    var platform: DevicePlatform = DevicePlatform.ANDROID
    var manufacturer: String = ""
    var model: String = ""
    var osName: String = ""
    var osVersion: String = ""
    var kernelVersion: String = ""
    var appVersion: String = ""
    var appBuildNumber: String = ""
    var language: String = ""
    var uptime: Long = 0L
    var cpuArch: String = ""
    var totalMemory: Long = 0L
    var totalStorage: Long = 0L
    var display: DDisplayInfo? = null
    var android: DAndroidDeviceInfo? = null
    var desktop: DDesktopDeviceInfo? = null
}

