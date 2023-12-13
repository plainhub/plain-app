package com.ismartcoding.plain.features.contact

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.normalizePhoneNumber
import com.ismartcoding.lib.extensions.queryCursor
import com.ismartcoding.plain.web.models.ContentItemInput
import com.ismartcoding.plain.web.models.OrganizationInput
import java.util.ArrayList

data class DContentItem(var value: String, var type: Int, var label: String)

data class DOrganization(var company: String, var title: String)

data class DContactPhoneNumber(var value: String, var type: Int, var label: String, var normalizedNumber: String)

data class Content(
    val events: MutableList<DContentItem> = ArrayList(),
    val websites: MutableList<DContentItem> = ArrayList(),
    val phoneNumbers: MutableList<DContactPhoneNumber> = ArrayList(),
    val emails: MutableList<DContentItem> = ArrayList(),
    val addresses: MutableList<DContentItem> = ArrayList(),
    val nicknames: MutableList<String> = ArrayList(),
    val ims: MutableList<DContentItem> = ArrayList(),
    val organizations: MutableList<DOrganization> = ArrayList(),
    val notes: MutableList<String> = ArrayList(),
    val groupIds: MutableList<Int> = ArrayList(),
)

object ContentHelper {
    fun getMap(context: Context): Map<String, Content> {
        val map = mutableMapOf<String, Content>()
        val uri = ContactsContract.Data.CONTENT_URI
        val projection =
            arrayOf(
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4,
                ContactsContract.Data.DATA5,
                ContactsContract.Data.DATA6,
            )

        context.queryCursor(uri, projection) { cursor, cache ->
            val id = cursor.getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache)
            if (map[id] == null) {
                map[id] = Content()
            }

            when (cursor.getStringValue(ContactsContract.Data.MIMETYPE, cache)) {
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                    val startDate = cursor.getStringValue(ContactsContract.CommonDataKinds.Event.START_DATE, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Event.TYPE, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.Event.LABEL, cache)
                    map[id]?.events?.add(DContentItem(startDate, type, label))
                }
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                    val address = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, cache)
                    map[id]?.addresses?.add(DContentItem(address, type, label))
                }
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    val email = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.DATA, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Email.TYPE, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.Email.LABEL, cache)
                    map[id]?.emails?.add(DContentItem(email, type, label))
                }
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    val number = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cache)
                    val normalizedNumber = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Phone.TYPE, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.Phone.LABEL, cache)
                    map[id]?.phoneNumbers?.add(DContactPhoneNumber(number, type, label, normalizedNumber))
                }
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                    val url = cursor.getStringValue(ContactsContract.CommonDataKinds.Website.URL, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Website.TYPE, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.Website.LABEL, cache)
                    map[id]?.websites?.add(DContentItem(url, type, label))
                }
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                    val name = cursor.getStringValue(ContactsContract.CommonDataKinds.Nickname.NAME, cache)
                    map[id]?.nicknames?.add(name)
                }
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                    val value = cursor.getStringValue(ContactsContract.CommonDataKinds.Im.DATA, cache)
                    val type = cursor.getIntValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, cache)
                    val label = cursor.getStringValue(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, cache)
                    map[id]?.ims?.add(DContentItem(value, type, label))
                }
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                    val note = cursor.getStringValue(ContactsContract.CommonDataKinds.Note.NOTE, cache)
                    map[id]?.notes?.add(note)
                }
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                    val company = cursor.getStringValue(ContactsContract.CommonDataKinds.Organization.COMPANY, cache)
                    val title = cursor.getStringValue(ContactsContract.CommonDataKinds.Organization.TITLE, cache)
                    map[id]?.organizations?.add(DOrganization(company, title))
                }
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
                    val groupId = cursor.getIntValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, cache)
                    map[id]?.groupIds?.add(groupId)
                }
            }
        }

        return map
    }

    fun newDelete(
        contactId: String,
        mimeType: String,
    ): ContentProviderOperation {
        val o = ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
        o.withSelection(
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? ",
            arrayOf(contactId, mimeType),
        )
        return o.build()
    }

    fun newInsert(
        contactId: String,
        mimeType: String,
        item: ContentItemInput,
    ): ContentProviderOperation {
        val o = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        o.apply {
            withValueId(this, contactId)
            withValue(ContactsContract.Data.MIMETYPE, mimeType)
            when (mimeType) {
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Email.DATA, item.value)
                    withValue(ContactsContract.CommonDataKinds.Email.TYPE, item.type)
                    withValue(ContactsContract.CommonDataKinds.Email.LABEL, item.label)
                }
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, item.value)
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, item.type)
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, item.label)
                }
                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Im.DATA, item.value)
                    withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, item.type)
                    withValue(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, item.label)
                }
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Event.START_DATE, item.value)
                    withValue(ContactsContract.CommonDataKinds.Event.TYPE, item.type)
                    withValue(ContactsContract.CommonDataKinds.Event.LABEL, item.label)
                }
                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Website.URL, item.value)
                    withValue(ContactsContract.CommonDataKinds.Website.TYPE, item.type)
                    withValue(ContactsContract.CommonDataKinds.Website.LABEL, item.label)
                }
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, item.value)
                    withValue(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, item.value.normalizePhoneNumber())
                    withValue(ContactsContract.CommonDataKinds.Phone.TYPE, item.type)
                    withValue(ContactsContract.CommonDataKinds.Phone.LABEL, item.label)
                }
            }
        }
        return o.build()
    }

    fun newInsert(
        contactId: String,
        mimeType: String,
        value: String,
    ): ContentProviderOperation {
        val o = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        o.apply {
            withValueId(this, contactId)
            withValue(ContactsContract.Data.MIMETYPE, mimeType)
            when (mimeType) {
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Note.NOTE, value)
                }
                ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Nickname.NAME, value)
                }
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, value)
                }
            }
        }
        return o.build()
    }

    fun newOrgInsert(
        contactId: String,
        organization: OrganizationInput,
    ): ContentProviderOperation {
        val o = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        o.apply {
            withValueId(this, contactId)
            withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
            withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organization.company)
            withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
            withValue(ContactsContract.CommonDataKinds.Organization.TITLE, organization.title)
        }
        return o.build()
    }

    private fun withValueId(
        builder: ContentProviderOperation.Builder,
        contactId: String,
    ) {
        if (contactId == "0") {
            builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        } else {
            builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId)
        }
    }
}
