package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ContactDetailFacts(
    val value: String,
    val type: Int,
    val label: String,
)
