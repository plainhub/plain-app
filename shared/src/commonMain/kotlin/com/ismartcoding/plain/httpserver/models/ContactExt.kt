package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.helpers.getFileId

fun DContact.toModel(): Contact {
    return Contact(
        ID(id), prefix, givenName, middleName, familyName, suffix,
        nickname, getFileId(photoUri), phoneNumbers.map { it.toModel() }, emails.map { it.toEmailModel() }, addresses.map { it.toAddressModel() },
        events.map { it.toEventModel() }, source,
        starred == 1, ID(contactId), getFileId(thumbnailUri),
        notes, groups.map { it.toModel() }, organization?.toModel(), websites.map { it.toWebsiteModel() }, ims.map { it.toImModel() }, ringtone, updatedAt,
    )
}
