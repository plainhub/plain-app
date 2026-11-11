package com.ismartcoding.plain.features.contacts

import com.ismartcoding.plain.features.telephony.ContactImProtocol
import kotlinx.serialization.Serializable

@Serializable
data class ContactImInput(var value: String, var protocol: ContactImProtocol, var customProtocol: String)
