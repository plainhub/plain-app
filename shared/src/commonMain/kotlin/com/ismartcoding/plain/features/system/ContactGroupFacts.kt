package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ContactGroupFacts(
    val id: String,
    val name: String,
)
