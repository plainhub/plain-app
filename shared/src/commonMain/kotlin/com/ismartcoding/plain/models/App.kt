package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.AppChannelType
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.permissions.Permission
import kotlinx.serialization.Serializable

@Serializable
data class App(
    var clientId: String,
    var urlToken: String,
    var httpPort: Int,
    var httpsPort: Int,
    var appDir: String,
    var deviceName: String,
    var deviceType: DeviceType,
    var capabilities: List<Capability>,
    var buildChannel: AppChannelType,
    var permissions: List<Permission>,
    var downloadsDir: String,
    var developerMode: Boolean,
    var debug: Boolean,
    var tlsCertificateSha256: String? = null,
)
