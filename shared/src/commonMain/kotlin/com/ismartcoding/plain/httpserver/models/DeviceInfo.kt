package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DDeviceStatus
import com.ismartcoding.plain.data.DTemperature
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
class AndroidExtras {
    var sdkVersion: Int = 0
    var versionCodeName: String = ""
    var securityPatch: String = ""
    var bootloader: String = ""
    var fingerprint: String = ""
    var hardware: String = ""
    var radioVersion: String = ""
    var board: String = ""
    var buildBrand: String = ""
    var buildNumber: String = ""
    var device: String = ""
    var javaVmVersion: String = ""
    var glEsVersion: String = ""
    var buildTime: Instant = TimeHelper.now()
}

@GraphQLType
class DisplayInfo {
    var width: Int = 0
    var height: Int = 0
    var density: Float = 0f
}

@GraphQLType
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
    var cpuArch: String = ""
    var cpuModel: String? = null
    var totalMemory: Long = 0L
    var totalStorage: Long = 0L
    var display: DisplayInfo? = null
    @GraphQLField(description = "Android-only build/runtime details; null on non-Android devices.")
    var android: AndroidExtras? = null
}

@GraphQLType
class Temperature {
    var label: String = ""
    var celsius: Double = 0.0
}

@GraphQLType
class DeviceStatus {
    var uptimeSec: Long = 0L
    @GraphQLField(description = "Battery percentage; null on devices without a battery (desktop/NAS).")
    var batteryLevel: Int? = null
    var charging: Boolean = false
    var temperatures: List<Temperature> = emptyList()
    var cpuUsage: Double = 0.0
    @GraphQLField(description = "Available memory in bytes; null when the platform cannot report it.")
    var memoryAvailable: Long? = null
    var storageAvailable: Long = 0L
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
    m.cpuArch = this.cpuArch
    m.cpuModel = this.cpuModel.ifEmpty { null }
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
        val ma = AndroidExtras()
        ma.sdkVersion = a.sdkVersion
        ma.versionCodeName = a.versionCodeName
        ma.securityPatch = a.securityPatch
        ma.bootloader = a.bootloader
        ma.fingerprint = a.fingerprint
        ma.hardware = a.hardware
        ma.radioVersion = a.radioVersion
        ma.board = a.board
        ma.buildBrand = a.buildBrand
        ma.buildNumber = a.buildNumber
        ma.device = a.device
        ma.javaVmVersion = a.javaVmVersion
        ma.glEsVersion = a.glEsVersion
        ma.buildTime = a.buildTime
        m.android = ma
    }
    return m
}

fun DTemperature.toModel(): Temperature {
    val m = Temperature()
    m.label = this.label
    m.celsius = this.celsius
    return m
}

fun DDeviceStatus.toModel(): DeviceStatus {
    val m = DeviceStatus()
    m.uptimeSec = this.uptimeSec
    m.batteryLevel = this.batteryLevel
    m.charging = this.charging
    m.temperatures = this.temperatures.map { it.toModel() }
    m.cpuUsage = this.cpuUsage
    m.memoryAvailable = this.memoryAvailable
    m.storageAvailable = this.storageAvailable
    return m
}
