package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.contact.*
import com.ismartcoding.plain.helpers.FileHelper
import kotlinx.datetime.Instant

data class ContentItem(var value: String, var type: Int, var label: String)

fun DContentItem.toModel(): ContentItem {
    return ContentItem(value, type, label)
}


data class Organization(var company: String, var title: String)

fun DOrganization.toModel(): Organization {
    return Organization(company, title)
}

data class ContactPhoneNumber(var value: String, var type: Int, var label: String, var normalizedNumber: String)

fun DContactPhoneNumber.toModel(): ContactPhoneNumber {
    return ContactPhoneNumber(value, type, label, normalizedNumber)
}

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

fun DContact.toModel(): Contact {
    return Contact(
        ID(id), prefix, givenName, middleName, familyName, suffix,
        nickname, FileHelper.getFileId(photoUri), phoneNumbers.map { it.toModel() }, emails.map { it.toModel() }, addresses.map { it.toModel() },
        events.map { it.toModel() }, source,
        starred == 1, ID(contactId), FileHelper.getFileId(thumbnailUri),
        notes, groups.map { it.toModel() }, organization?.toModel(), websites.map { it.toModel() }, ims.map { it.toModel() }, ringtone, updatedAt,
    )
}
