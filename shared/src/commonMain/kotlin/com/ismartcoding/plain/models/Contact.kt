package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    override var id: String,
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
    var contactId: String,
    var thumbnailId: String,
    var notes: String,
    var groups: List<ContactGroup>,
    var organization: ContactOrganization?,
    var websites: List<ContactWebsite>,
    var ims: List<ContactIm>,
    var ringtone: String,
    var updatedAt: Instant,
) : IData
