package com.ismartcoding.plain.features.contacts

import kotlinx.serialization.Serializable

import com.ismartcoding.plain.lib.ID

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
    var organization: ContactOrganizationInput?,
    var websites: List<ContactWebsiteInput>,
    var ims: List<ContactImInput>,
)
