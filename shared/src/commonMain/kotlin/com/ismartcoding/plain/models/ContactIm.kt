package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.telephony.ContactImProtocol
import kotlinx.serialization.Serializable

@Serializable
data class ContactIm(
    var value: String,
    var protocol: ContactImProtocol,
    var customProtocol: String,
    var typeCode: Int = 0,
)
