package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactWebsiteType
import kotlinx.serialization.Serializable

@Serializable
data class ContactWebsiteInput(
    var value: String,
    var type: ContactWebsiteType,
    var label: String,
)
