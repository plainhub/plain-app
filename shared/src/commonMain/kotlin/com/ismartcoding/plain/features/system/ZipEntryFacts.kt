package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ZipEntryFacts(
    val path: String,
    val name: String,
)
