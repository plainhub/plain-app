package com.ismartcoding.plain.platform.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.ismartcoding.plain.lib.extensions.forEach
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.normalizePhoneNumber
import com.ismartcoding.plain.lib.extensions.queryCursor
import com.ismartcoding.plain.features.contacts.ContactAddressInput
import com.ismartcoding.plain.features.contacts.ContactEmailInput
import com.ismartcoding.plain.features.contacts.ContactEventInput
import com.ismartcoding.plain.features.contacts.ContactImInput
import com.ismartcoding.plain.features.contacts.ContactPhoneInput
import com.ismartcoding.plain.features.contacts.ContactWebsiteInput
import com.ismartcoding.plain.features.contacts.ContactOrganizationInput
import java.util.ArrayList
import com.ismartcoding.plain.rust.host.provider.ContactDataFacts

internal object ContactDataHelper {
    fun rows(context: Context, ids: List<String>): List<ContactDataFacts> =
        ids.distinct().chunked(128).flatMap { selected ->
            val projection = arrayOf(ContactsContract.Data.RAW_CONTACT_ID, ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.DATA1, ContactsContract.Data.DATA2, ContactsContract.Data.DATA3, ContactsContract.Data.DATA4, ContactsContract.Data.DATA5, ContactsContract.Data.DATA6)
            val placeholders = selected.joinToString(",") { "?" }
            val rows = ArrayList<ContactDataFacts>()
            context.contentResolver.queryCursor(ContactsContract.Data.CONTENT_URI, projection,
                "${ContactsContract.Data.RAW_CONTACT_ID} IN ($placeholders)", selected.toTypedArray())?.forEach { cursor, cache ->
                rows.add(ContactDataFacts(
                    cursor.getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache),
                    cursor.getStringValue(ContactsContract.Data.MIMETYPE, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA1, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA2, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA3, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA4, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA5, cache),
                    cursor.getStringValue(ContactsContract.Data.DATA6, cache),
                ))
            }
            rows
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

    fun newInsert(contactId: String, mimeType: String, item: ContactPhoneInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.type.androidValue, item.label)

    fun newInsert(contactId: String, mimeType: String, item: ContactEmailInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.type.androidValue, item.label)

    fun newInsert(contactId: String, mimeType: String, item: ContactAddressInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.type.androidValue, item.label)

    fun newInsert(contactId: String, mimeType: String, item: ContactEventInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.type.androidValue, item.label)

    fun newInsert(contactId: String, mimeType: String, item: ContactWebsiteInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.type.androidValue, item.label)

    fun newInsert(contactId: String, mimeType: String, item: ContactImInput): ContentProviderOperation =
        newInsert(contactId, mimeType, item.value, item.protocol.androidValue, item.customProtocol)

    private fun newInsert(
        contactId: String,
        mimeType: String,
        value: String,
        typeValue: Int,
        label: String,
    ): ContentProviderOperation {
        val o = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        o.apply {
            withValueId(this, contactId)
            withValue(ContactsContract.Data.MIMETYPE, mimeType)
            when (mimeType) {
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Email.DATA, value)
                    withValue(ContactsContract.CommonDataKinds.Email.TYPE, typeValue)
                    withValue(ContactsContract.CommonDataKinds.Email.LABEL, label)
                }

                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, value)
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, typeValue)
                    withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, label)
                }

                ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Im.DATA, value)
                    withValue(ContactsContract.CommonDataKinds.Im.PROTOCOL, typeValue)
                    withValue(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL, label)
                }

                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Event.START_DATE, value)
                    withValue(ContactsContract.CommonDataKinds.Event.TYPE, typeValue)
                    withValue(ContactsContract.CommonDataKinds.Event.LABEL, label)
                }

                ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Website.URL, value)
                    withValue(ContactsContract.CommonDataKinds.Website.TYPE, typeValue)
                    withValue(ContactsContract.CommonDataKinds.Website.LABEL, label)
                }

                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                    withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, value)
                    withValue(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER, value.normalizePhoneNumber())
                    withValue(ContactsContract.CommonDataKinds.Phone.TYPE, typeValue)
                    withValue(ContactsContract.CommonDataKinds.Phone.LABEL, label)
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
        organization: ContactOrganizationInput,
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
