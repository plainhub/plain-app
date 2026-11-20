package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactDateType
import kotlinx.serialization.Serializable

@Serializable
data class ContactEvent(
    var value: String,
    var type: ContactDateType,
    var label: String,
)
