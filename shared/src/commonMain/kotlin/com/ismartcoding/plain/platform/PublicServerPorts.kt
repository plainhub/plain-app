package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class PublicServerPorts(val httpPort: Int, val httpsPort: Int)
