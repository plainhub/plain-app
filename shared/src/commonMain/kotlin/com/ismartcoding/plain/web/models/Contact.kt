package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.features.contact.DContactPhoneNumber
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.features.contact.DOrganization
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class ContentItem(var value: String, var type: Int, var label: String)

fun DContentItem.toModel(): ContentItem {
    return ContentItem(value, type, label)
}

@GraphQLType
data class Organization(var company: String, var title: String)

fun DOrganization.toModel(): Organization {
    return Organization(company, title)
}

@GraphQLType
data class ContactPhoneNumber(var value: String, var type: Int, var label: String, var normalizedNumber: String)

fun DContactPhoneNumber.toModel(): ContactPhoneNumber {
    return ContactPhoneNumber(value, type, label, normalizedNumber)
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
    var emails: List<ContentItem>,
    var addresses: List<ContentItem>,
    var events: List<ContentItem>,
    var source: String,
    var starred: Boolean,
    var contactId: ID,
    var thumbnailId: String,
    var notes: String,
    var groups: List<ContactGroup>,
    var organization: Organization?,
    var websites: List<ContentItem>,
    var ims: List<ContentItem>,
    var ringtone: String,
    var updatedAt: Instant,
)
