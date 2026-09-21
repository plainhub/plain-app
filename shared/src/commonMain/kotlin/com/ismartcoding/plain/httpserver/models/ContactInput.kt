package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.EmailType
import com.ismartcoding.plain.enums.EventType
import com.ismartcoding.plain.enums.ImProtocol
import com.ismartcoding.plain.enums.PhoneType
import com.ismartcoding.plain.enums.PostalType
import com.ismartcoding.plain.enums.WebsiteType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class ContactPhoneInput(var value: String, var type: PhoneType, var label: String)

@GraphQLInput
@Serializable
data class ContactEmailInput(var value: String, var type: EmailType, var label: String)

@GraphQLInput
@Serializable
data class ContactAddressInput(var value: String, var type: PostalType, var label: String)

@GraphQLInput
@Serializable
data class ContactEventInput(var value: String, var type: EventType, var label: String)

@GraphQLInput
@Serializable
data class ContactWebsiteInput(var value: String, var type: WebsiteType, var label: String)

@GraphQLInput
@Serializable
data class ContactImInput(var value: String, var protocol: ImProtocol, var customProtocol: String)
@GraphQLInput
@Serializable
data class OrganizationInput(var company: String, var title: String)

@GraphQLInput
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
