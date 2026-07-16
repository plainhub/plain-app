package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.platform.getFileId

fun DContact.toModel(): Contact {
    return Contact(
        ID(id), prefix, givenName, middleName, familyName, suffix,
        nickname, getFileId(photoUri), phoneNumbers.map { it.toModel() }, emails.map { it.toModel() }, addresses.map { it.toModel() },
        events.map { it.toModel() }, source,
        starred == 1, ID(contactId), getFileId(thumbnailUri),
        notes, groups.map { it.toModel() }, organization?.toModel(), websites.map { it.toModel() }, ims.map { it.toModel() }, ringtone, updatedAt,
    )
}
