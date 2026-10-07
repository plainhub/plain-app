package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class PackageInstallFacts(
    val packageName: String,
    val lastUpdateTime: String?,
    val isNew: Boolean,
)
