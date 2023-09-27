package com.ismartcoding.plain.features.contact

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.BaseContentHelper
import com.ismartcoding.plain.web.models.ContactInput
import java.util.*

object ContactHelper : BaseContentHelper() {
    override val uriExternal: Uri = ContactsContract.Data.CONTENT_URI
    override val idKey: String = ContactsContract.Data.RAW_CONTACT_ID

    override fun getProjection(): Array<String> {
        return arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.RAW_CONTACT_ID,
            ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.CommonDataKinds.StructuredName.PREFIX,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
            ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI,
            ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.StructuredName.STARRED,
            ContactsContract.CommonDataKinds.StructuredName.CUSTOM_RINGTONE,
            ContactsContract.RawContacts.ACCOUNT_NAME,
            ContactsContract.RawContacts.ACCOUNT_TYPE,
        )
    }

    override fun getWhere(query: String): ContentWhere {
        val where = ContentWhere()
        where.add("${ContactsContract.Data.MIMETYPE} = ?", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            queryGroups.forEach {
                if (it.name == "text") {
                    where.add("${ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME} LIKE ?", "%${it.value}%")
                } else if (it.name == "ids") {
                    val ids = it.value.split(",")
                    if (ids.isNotEmpty()) {
                        where.addIn(ContactsContract.Data.RAW_CONTACT_ID, ids)
                    }
                }
            }
        }

        return where
    }

    override fun deleteByIds(
        context: Context,
        ids: Set<String>,
    ) {
        ids.chunked(30).forEach { chunk ->
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} IN (${StringHelper.getQuestionMarks(chunk.size)})"
            val selectionArgs = chunk.map { it }.toTypedArray()
            context.contentResolver.delete(uriExternal, selection, selectionArgs)
        }
    }

    fun get(
        context: Context,
        id: String,
    ): DContact? {
        return search(context, "ids=$id", 1, 0).firstOrNull()
    }

    fun update(
        id: String,
        contact: ContactInput,
    ) {
        val context = MainApp.instance
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI).apply {
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            withSelection(selection, selectionArgs)
            withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, contact.prefix)
            withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.lastName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
            operations.add(build())
        }

        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.add(ContentHelper.newInsert(id, type, contact.nickname))
        }

        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.phoneNumbers.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.emails.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.addresses.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.ims.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.events.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.add(ContentHelper.newInsert(id, type, contact.notes))
        }

        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            contact.organization?.let {
                operations.add(ContentHelper.newOrgInsert(id, it))
            }
        }

        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.websites.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newDelete(id, type))
            operations.addAll(contact.groupIds.map { ContentHelper.newInsert(id, type, it.value) })
        }

//            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toString())
//            val contentValues = ContentValues(2)
//            contentValues.put(ContactsContract.Contacts.STARRED, contact.starred)
//            contentValues.put(ContactsContract.Contacts.CUSTOM_RINGTONE, contact.ringtone)
//            context.contentResolver.update(uri, contentValues, null, null)

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }

    fun create(contact: ContactInput): String {
        val context = MainApp.instance
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
            withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.source)
            val source = SourceHelper.getAll().find { it.name == contact.source }
            withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, source!!.type)
            operations.add(build())
        }

        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).apply {
            withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, contact.prefix)
            withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contact.middleName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.lastName)
            withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, contact.suffix)
            operations.add(build())
        }

        val id = "0"
        ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newInsert(id, type, contact.nickname))
        }

        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.phoneNumbers.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.emails.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.addresses.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.ims.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.events.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContentHelper.newInsert(id, type, contact.notes))
        }

        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE.let {
            contact.organization?.let {
                operations.add(ContentHelper.newOrgInsert(id, it))
            }
        }

        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.websites.map { ContentHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.groupIds.map { ContentHelper.newInsert(id, type, it.value) })
        }

        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        if (results.isEmpty()) {
            return ""
        }

        return ContentUris.parseId(results[0].uri!!).toString()
    }

    fun search(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DContact> {
        val contentMap = ContentHelper.getMap(context)
        val cursor =
            getSearchCursorWithSortOrder(
                context,
                query,
                limit,
                offset,
                SortBy(ContactsContract.Data.DISPLAY_NAME_PRIMARY, SortDirection.ASC),
            )
        val items = mutableListOf<DContact>()
        if (cursor?.moveToFirst() == true) {
            val cache = mutableMapOf<String, Int>()
            do {
                val accountName = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME, cache)
                val rawId = cursor.getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache)
                val prefix = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, cache)
                val givenName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, cache)
                val middleName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, cache)
                val familyName = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, cache)
                val suffix = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, cache)
                val photoUri = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI, cache)
                val starred = cursor.getIntValue(ContactsContract.CommonDataKinds.StructuredName.STARRED, cache)
                val contactId = cursor.getStringValue(ContactsContract.Data.CONTACT_ID, cache)
                val thumbnailUri = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI, cache)
                val ringtone = cursor.getStringValue(ContactsContract.CommonDataKinds.StructuredName.CUSTOM_RINGTONE, cache)
                val updatedAt = cursor.getTimeValue(ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP, cache)

                val nicknames = contentMap[rawId]?.nicknames ?: arrayListOf()
                val notes = contentMap[rawId]?.notes ?: arrayListOf()
                val groups = ArrayList<DGroup>()
                val organizations = contentMap[rawId]?.organizations ?: arrayListOf()
                val websites = contentMap[rawId]?.websites ?: arrayListOf()
                val events = contentMap[rawId]?.events ?: arrayListOf()
                val emails = contentMap[rawId]?.emails ?: arrayListOf()
                val addresses = contentMap[rawId]?.addresses ?: arrayListOf()
                val ims = contentMap[rawId]?.ims ?: arrayListOf()
                val phoneNumbers = contentMap[rawId]?.phoneNumbers ?: arrayListOf()
                val contact =
                    DContact(
                        rawId, prefix, givenName, middleName,
                        familyName, suffix,
                        if (nicknames.isNotEmpty()) nicknames[0] else "",
                        photoUri,
                        phoneNumbers,
                        emails,
                        addresses,
                        events,
                        accountName,
                        starred,
                        contactId, thumbnailUri,
                        if (notes.isNotEmpty()) notes[0] else "",
                        groups,
                        if (organizations.isNotEmpty()) organizations[0] else DOrganization("", ""),
                        websites,
                        ims,
                        ringtone,
                        updatedAt,
                    )

                items.add(contact)
            } while (cursor.moveToNext())
        }

        return items
    }
}
