package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.helpers.TimeHelper
import kotlin.time.Instant

class AndroidDeviceInfo {
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

class DesktopDeviceInfo {
    var hostname: String = ""
    var cpuModel: String = ""
    var gpuModel: String = ""
    var desktopEnvironment: String = ""
    var windowManager: String = ""
}

class DisplayInfo {
    var width: Int = 0
    var height: Int = 0
    var density: String = ""
}

class DeviceInfo {
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
    var display: DisplayInfo? = null
    var android: AndroidDeviceInfo? = null
    var desktop: DesktopDeviceInfo? = null
}

fun DDeviceInfo.toModel(): DeviceInfo {
    val m = DeviceInfo()
    m.name = this.name
    m.platform = this.platform
    m.manufacturer = this.manufacturer
    m.model = this.model
    m.osName = this.osName
    m.osVersion = this.osVersion
    m.kernelVersion = this.kernelVersion
    m.appVersion = this.appVersion
    m.appBuildNumber = this.appBuildNumber
    m.language = this.language
    m.uptime = this.uptime
    m.cpuArch = this.cpuArch
    m.totalMemory = this.totalMemory
    m.totalStorage = this.totalStorage
    this.display?.let { d ->
        val md = DisplayInfo()
        md.width = d.width
        md.height = d.height
        md.density = d.density
        m.display = md
    }
    this.android?.let { a ->
        val ma = AndroidDeviceInfo()
        ma.sdkVersion = a.sdkVersion
        ma.versionCodeName = a.versionCodeName
        ma.securityPatch = a.securityPatch
        ma.bootloader = a.bootloader
        ma.fingerprint = a.fingerprint
        ma.hardware = a.hardware
        ma.radioVersion = a.radioVersion
        ma.board = a.board
        ma.buildBrand = a.buildBrand
        ma.buildHost = a.buildHost
        ma.buildUser = a.buildUser
        ma.buildNumber = a.buildNumber
        ma.product = a.product
        ma.device = a.device
        ma.javaVmVersion = a.javaVmVersion
        ma.glEsVersion = a.glEsVersion
        ma.serial = a.serial
        ma.buildTime = a.buildTime
        m.android = ma
    }
    return m
}