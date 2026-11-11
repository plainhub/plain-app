package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class ContactOrganizationInput(
    var company: String,
    var title: String,
)
