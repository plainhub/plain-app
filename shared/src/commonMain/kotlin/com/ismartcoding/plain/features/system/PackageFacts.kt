package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class PackageFacts(
    val item: com.ismartcoding.plain.platform.DPackageInfo,
    val nameSortKey: String,
)
