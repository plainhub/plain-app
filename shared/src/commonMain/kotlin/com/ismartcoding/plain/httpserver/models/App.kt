package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.AppChannelType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.platform.DeviceFeature
import com.ismartcoding.plain.platform.Permission

@GraphQLType
data class App(
    val clientId: String,
    val urlToken: String,
    val httpPort: Int,
    val httpsPort: Int,
    val appDir: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val features: List<DeviceFeature>,
    val channel: AppChannelType,
    val permissions: List<Permission>,
    val downloadsDir: String,
    val developerMode: Boolean,
    val debug: Boolean,
)
