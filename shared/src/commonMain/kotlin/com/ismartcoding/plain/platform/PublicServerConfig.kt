package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class PublicServerConfig(val httpPort: Int, val httpsPort: Int, val debug: Boolean)
