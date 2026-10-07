package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class DocExtensionFacts(
    val ext: String,
    val count: Int,
)
