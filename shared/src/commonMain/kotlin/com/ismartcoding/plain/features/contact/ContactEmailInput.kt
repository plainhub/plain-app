package com.ismartcoding.plain.features.contact

import com.ismartcoding.plain.enums.EmailType
import com.ismartcoding.plain.enums.EventType
import com.ismartcoding.plain.enums.ImProtocol
import com.ismartcoding.plain.enums.PhoneType
import com.ismartcoding.plain.enums.PostalType
import com.ismartcoding.plain.enums.WebsiteType
import kotlinx.serialization.Serializable

import com.ismartcoding.plain.data.ID

@Serializable
data class ContactEmailInput(var value: String, var type: EmailType, var label: String)
