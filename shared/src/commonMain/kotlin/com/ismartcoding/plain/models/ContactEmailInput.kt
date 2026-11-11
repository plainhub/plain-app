package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactEmailType
import kotlinx.serialization.Serializable

@Serializable
data class ContactEmailInput(
    var value: String,
    var type: ContactEmailType,
    var label: String,
)
