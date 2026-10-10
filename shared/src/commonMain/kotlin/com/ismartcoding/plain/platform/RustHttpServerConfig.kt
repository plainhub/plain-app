package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class RustHttpServerConfig(
    val debug: Boolean,
    val deviceName: String,
    val webRoot: String,
)
