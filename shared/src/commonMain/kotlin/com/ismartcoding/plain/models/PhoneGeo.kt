package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class PhoneGeo(
    var country: String,
    var numberType: PhoneNumberType,
    var carrier: String,
    var description: String,
)
