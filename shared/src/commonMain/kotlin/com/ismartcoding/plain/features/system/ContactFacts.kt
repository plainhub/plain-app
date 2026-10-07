package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ContactFacts(
    val id: String,
    val prefix: String,
    val firstName: String,
    val middleName: String,
    val lastName: String,
    val suffix: String,
    val nickname: String,
    val photoId: String,
    val phoneNumbers: List<ContactPhoneFacts>,
    val emails: List<ContactDetailFacts>,
    val addresses: List<ContactDetailFacts>,
    val events: List<ContactDetailFacts>,
    val websites: List<ContactDetailFacts>,
    val ims: List<ContactDetailFacts>,
    val source: String,
    val starred: Boolean,
    val contactId: String,
    val thumbnailId: String,
    val notes: String,
    val groups: List<ContactGroupFacts>,
    val organization: ContactOrganizationFacts?,
    val ringtone: String,
    val updatedAt: String,
)
