package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ContactOrganizationFacts(
    val company: String,
    val title: String,
)
