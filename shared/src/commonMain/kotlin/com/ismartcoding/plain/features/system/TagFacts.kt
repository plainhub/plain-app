package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class TagFacts(
    val id: String,
    val name: String,
    val count: Int,
)
