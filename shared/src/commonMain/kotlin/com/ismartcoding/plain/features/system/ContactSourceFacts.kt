package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ContactSourceFacts(
    val name: String,
    val type: String,
)
