package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class ContactOrganization(
    var company: String,
    var title: String,
)
