package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactPostalType
import kotlinx.serialization.Serializable

@Serializable
data class ContactAddress(
    var value: String,
    var type: ContactPostalType,
    var label: String,
    var typeCode: Int = 0,
)
