package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.datetime.Instant

class DeviceInfo {
    var deviceName: String = ""
    var releaseBuildVersion: String = ""
    var versionCodeName: String = ""
    var securityPatch: String = ""
    var bootloader: String = ""
    var manufacturer: String = ""
    var deviceId: String = ""
    var model: String = ""
    var product: String = ""
    var fingerprint: String = ""
    var hardware: String = ""
    var radioVersion: String = ""
    var device: String = ""
    var board: String = ""
    var displayVersion: String = ""
    var buildBrand: String = ""
    var buildHost: String = ""
    var buildTime: Instant = TimeHelper.now()
    var uptime = 0L
    var buildUser: String = ""
    var serial: String = ""
    var osVersion: String = ""
    var language: String = ""
    var sdkVersion: Int = 0
    var screenDensity: String = ""
    var javaVmVersion: String = ""
    var kernelVersion: String = ""
    var glEsVersion: String = ""
    var screenHeight: Int = 0
    var screenWidth: Int = 0
    var phoneNumbers: List<PhoneNumber> = listOf()
}

data class PhoneNumber(
    val id: Int, // Slot Number
    val name: String, // SIM Provider
    val number: String, // Number
)

fun DDeviceInfo.toModel(): DeviceInfo {
    val model = DeviceInfo()
    model.deviceName = this.deviceName
    model.releaseBuildVersion = this.releaseBuildVersion
    model.versionCodeName = this.versionCodeName
    model.securityPatch = this.securityPatch
    model.bootloader = this.bootloader
    model.manufacturer = this.manufacturer
    model.deviceId = this.deviceId
    model.model = this.model
    model.product = this.product
    model.fingerprint = this.fingerprint
    model.hardware = this.hardware
    model.radioVersion = this.radioVersion
    model.device = this.device
    model.board = this.board
    model.displayVersion = this.displayVersion
    model.buildBrand = this.buildBrand
    model.buildHost = this.buildHost
    model.buildTime = this.buildTime
    model.uptime = this.uptime
    model.buildUser = this.buildUser
    model.serial = this.serial
    model.language = this.language
    model.sdkVersion = this.sdkVersion
    model.javaVmVersion = this.javaVmVersion
    model.kernelVersion = this.kernelVersion
    model.glEsVersion = this.glEsVersion
    model.screenDensity = this.screenDensity
    model.screenHeight = this.screenHeight
    model.screenWidth = this.screenWidth
    model.phoneNumbers = this.phoneNumbers.map { PhoneNumber(it.id, it.name, it.number) }
    return model
}