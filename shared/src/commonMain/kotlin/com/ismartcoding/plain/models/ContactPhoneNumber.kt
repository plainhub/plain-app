package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactPhoneType
import kotlinx.serialization.Serializable

@Serializable
data class ContactPhoneNumber(
    var value: String,
    var type: ContactPhoneType,
    var label: String,
    var normalizedNumber: String,
    var typeCode: Int = 0,
)
