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
data class ContactInput(
    var prefix: String,
    var firstName: String,
    var middleName: String,
    var lastName: String,
    var suffix: String,
    var nickname: String,
    var phoneNumbers: List<ContactPhoneInput>,
    var emails: List<ContactEmailInput>,
    var addresses: List<ContactAddressInput>,
    var events: List<ContactEventInput>,
    var source: String,
    var starred: Boolean,
    var notes: String,
    var groupIds: List<ID>,
    var organization: OrganizationInput?,
    var websites: List<ContactWebsiteInput>,
    var ims: List<ContactImInput>,
)
