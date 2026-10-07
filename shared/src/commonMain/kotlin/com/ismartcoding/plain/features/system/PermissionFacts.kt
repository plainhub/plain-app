package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class PermissionFacts(
    val granted: Map<String, Boolean>,
)
