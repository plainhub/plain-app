package com.ismartcoding.plain.features.contacts

import com.ismartcoding.plain.features.telephony.ContactPostalType
import kotlinx.serialization.Serializable

@Serializable
data class ContactAddressInput(var value: String, var type: ContactPostalType, var label: String)
