package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.enums.EmailType
import com.ismartcoding.plain.enums.EventType
import com.ismartcoding.plain.enums.ImProtocol
import com.ismartcoding.plain.enums.PhoneType
import com.ismartcoding.plain.enums.PostalType
import com.ismartcoding.plain.enums.WebsiteType
import com.ismartcoding.plain.features.contact.DContactPhoneNumber
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.features.contact.DOrganization
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class ContactEmail(var value: String, var type: EmailType, var label: String)

fun DContentItem.toEmailModel(): ContactEmail {
    return ContactEmail(value, EmailType.fromInt(type), label)
}

@GraphQLType
data class ContactAddress(var value: String, var type: PostalType, var label: String)

fun DContentItem.toAddressModel(): ContactAddress {
    return ContactAddress(value, PostalType.fromInt(type), label)
}

@GraphQLType
data class ContactEvent(var value: String, var type: EventType, var label: String)

fun DContentItem.toEventModel(): ContactEvent {
    return ContactEvent(value, EventType.fromInt(type), label)
}

@GraphQLType
data class ContactWebsite(var value: String, var type: WebsiteType, var label: String)

fun DContentItem.toWebsiteModel(): ContactWebsite {
    return ContactWebsite(value, WebsiteType.fromInt(type), label)
}

@GraphQLType
data class ContactIm(var value: String, var protocol: ImProtocol, var customProtocol: String)

fun DContentItem.toImModel(): ContactIm {
    return ContactIm(value, ImProtocol.fromInt(type), label)
}

@GraphQLType
data class Organization(var company: String, var title: String)

fun DOrganization.toModel(): Organization {
    return Organization(company, title)
}

@GraphQLType
data class ContactPhoneNumber(var value: String, var type: PhoneType, var label: String, var normalizedNumber: String)

fun DContactPhoneNumber.toModel(): ContactPhoneNumber {
    return ContactPhoneNumber(value, PhoneType.fromInt(type), label, normalizedNumber)
}

@GraphQLType
data class Contact(
    var id: ID,
    var prefix: String,
    var firstName: String,
    var middleName: String,
    var lastName: String,
    var suffix: String,
    var nickname: String,
    var photoId: String,
    var phoneNumbers: List<ContactPhoneNumber>,
    var emails: List<ContactEmail>,
    var addresses: List<ContactAddress>,
    var events: List<ContactEvent>,
    var source: String,
    var starred: Boolean,
    var contactId: ID,
    var thumbnailId: String,
    var notes: String,
    var groups: List<ContactGroup>,
    var organization: Organization?,
    var websites: List<ContactWebsite>,
    var ims: List<ContactIm>,
    var ringtone: String,
    var updatedAt: Instant,
)
