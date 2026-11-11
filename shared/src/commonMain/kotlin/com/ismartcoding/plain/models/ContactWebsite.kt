package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactWebsiteType
import kotlinx.serialization.Serializable

@Serializable
data class ContactWebsite(
    var value: String,
    var type: ContactWebsiteType,
    var label: String,
    var typeCode: Int = 0,
)
