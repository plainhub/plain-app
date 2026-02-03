package com.ismartcoding.plain.data

import com.ismartcoding.plain.web.models.DPhoneNumber
import kotlinx.datetime.Instant
import com.ismartcoding.plain.helpers.TimeHelper

class DDeviceInfo {
    var deviceName = ""
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
    var uptime: Long = 0L
    var buildUser: String = ""
    var serial: String = ""
    var language: String = ""
    var sdkVersion: Int = 0
    var screenDensity: String = ""
    var javaVmVersion: String = ""
    var kernelVersion: String = ""
    var glEsVersion: String = ""
    var screenHeight: Int = 0
    var screenWidth: Int = 0
    var phoneNumbers: List<DPhoneNumber> = listOf()
}

