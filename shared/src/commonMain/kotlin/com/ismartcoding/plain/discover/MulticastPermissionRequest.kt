package com.ismartcoding.plain.discover

import kotlinx.serialization.Serializable

@Serializable
internal data class MulticastPermissionRequest(val lease: String, val acquire: Boolean)
