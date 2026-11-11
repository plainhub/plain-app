package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactPhoneType
import kotlinx.serialization.Serializable

@Serializable
data class ContactPhoneInput(
    var value: String,
    var type: ContactPhoneType,
    var label: String,
)
