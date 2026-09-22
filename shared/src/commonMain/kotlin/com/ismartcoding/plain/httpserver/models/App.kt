package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.AppChannelType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import com.ismartcoding.plain.platform.Capability
import com.ismartcoding.plain.platform.Permission

@GraphQLType
data class App(
    val clientId: String,
    @GraphQLField(description = "Base64 key used to encrypt/decrypt URL parameters for this server's web access (see file display URLs). Treat as a secret for the current session.")
    val urlToken: String,
    val httpPort: Int,
    val httpsPort: Int,
    val appDir: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val capabilities: List<Capability>,
    val channel: AppChannelType,
    val permissions: List<Permission>,
    val downloadsDir: String,
    val developerMode: Boolean,
    val debug: Boolean,
)
