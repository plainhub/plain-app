package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ZipItemsFacts(
    val items: List<ZipEntryFacts>,
)
