package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class ResourceStreamEnd(val kind: String = "end")
