package com.ismartcoding.plain.features.system

import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.features.contact.DContentItem
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.json.*

internal object SystemContactsHost {
    suspend fun handle(method: String, params: JsonObject): JsonElement = when (method) {
        "systemContactFacts" -> {
            val query = params.getValue("query").jsonPrimitive.content
            val offset = params.getValue("offset").jsonPrimitive.int
            val limit = params.getValue("limit").jsonPrimitive.int
            JsonHelper.jsonEncodeToElement(
                com.ismartcoding.plain.platform.searchMedia(
                    com.ismartcoding.plain.enums.DataType.CONTACT, query, limit, offset,
                    com.ismartcoding.plain.features.file.FileSortBy.DATE_DESC,
                ).filterIsInstance<DContact>().map(::contactFacts)
            )
        }
        "systemContactCount" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.countMedia(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemContactIds" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getMediaIds(
                com.ismartcoding.plain.enums.DataType.CONTACT,
                params.getValue("query").jsonPrimitive.content,
            )
        )
        "systemContactGroupFacts" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getContactGroups().map { group ->
                ContactGroupFacts(
                    id = group.id.toString(),
                    name = group.name,
                )
            }
        )
        "systemContactSources" -> JsonHelper.jsonEncodeToElement(
            com.ismartcoding.plain.platform.getContactSources().map { source ->
                ContactSourceFacts(
                    name = source.name,
                    type = source.type,
                )
            }
        )
        "systemCreateContact" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createContact(
            contactInput(params)))
        "systemUpdateContact" -> {
            com.ismartcoding.plain.platform.updateContact(
                params.getValue("id").jsonPrimitive.content,
                contactInput(params),
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemCreateContactGroup" -> JsonHelper.jsonEncodeToElement(com.ismartcoding.plain.platform.createContactGroup(
            params.getValue("name").jsonPrimitive.content,
            params.getValue("accountName").jsonPrimitive.content,
            params.getValue("accountType").jsonPrimitive.content,
            ).id.toString())
        "systemUpdateContactGroup" -> {
            com.ismartcoding.plain.platform.updateContactGroup(
                params.getValue("id").jsonPrimitive.content,
                params.getValue("name").jsonPrimitive.content,
            )
            JsonHelper.jsonEncodeToElement(true)
        }
        "systemDeleteContactGroup" -> {
            com.ismartcoding.plain.platform.deleteContactGroup(params.getValue("id").jsonPrimitive.content)
            JsonHelper.jsonEncodeToElement(true)
        }
        else -> error("Unsupported provider operation")
    }

    private fun contactInput(params: JsonObject): com.ismartcoding.plain.httpserver.models.ContactInput =
        JsonHelper.jsonDecodeFromElement(params.getValue("input"))

    /**
     * The public contact contract, field for field. The `ContactsContract`
     * `DATA2` code stays a raw int on each detail row: Rust owns the enum, so the
     * wire keeps the number and the mapping stays in one place.
     */
    private fun contactFacts(contact: DContact): ContactFacts {
        fun contentItem(item: DContentItem): ContactDetailFacts = ContactDetailFacts(
            value = item.value,
            type = item.type,
            label = item.label,
        )
        fun contentItems(items: List<DContentItem>): List<ContactDetailFacts> = items.map(::contentItem)
        return ContactFacts(
            id = contact.id,
            prefix = contact.prefix,
            firstName = contact.givenName,
            middleName = contact.middleName,
            lastName = contact.familyName,
            suffix = contact.suffix,
            nickname = contact.nickname,
            photoId = contact.photoUri,
            phoneNumbers = contact.phoneNumbers.map { phone ->
                ContactPhoneFacts(
                    value = phone.value,
                    type = phone.type,
                    label = phone.label,
                    normalizedNumber = phone.normalizedNumber,
                )
            },
            emails = contentItems(contact.emails),
            addresses = contentItems(contact.addresses),
            events = contentItems(contact.events),
            websites = contentItems(contact.websites),
            ims = contentItems(contact.ims),
            source = contact.source,
            starred = contact.starred != 0,
            contactId = contact.contactId,
            thumbnailId = contact.thumbnailUri,
            notes = contact.notes,
            groups = contact.groups.map { group ->
                ContactGroupFacts(
                    id = group.id.toString(),
                    name = group.name,
                )
            },
            organization = contact.organization?.let { organization ->
                ContactOrganizationFacts(
                    company = organization.company,
                    title = organization.title,
                )
            },
            ringtone = contact.ringtone,
            updatedAt = contact.updatedAt.toString(),
        )
    }
}
