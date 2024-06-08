package com.ismartcoding.plain.features.media

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.provider.ContactsContract
import com.ismartcoding.lib.content.ContentWhere
import com.ismartcoding.lib.data.SortBy
import com.ismartcoding.lib.data.enums.SortDirection
import com.ismartcoding.lib.extensions.count
import com.ismartcoding.lib.extensions.getIntValue
import com.ismartcoding.lib.extensions.getPagingCursor
import com.ismartcoding.lib.extensions.getSearchCursor
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.getTimeValue
import com.ismartcoding.lib.extensions.map
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.data.DGroup
import com.ismartcoding.plain.features.contact.ContentHelper
import com.ismartcoding.plain.features.contact.DOrganization
import com.ismartcoding.plain.features.contact.SourceHelper
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.web.models.ContactInput

object ContactMediaStoreHelper {
    private val uriExternal: Uri = ContactsContract.Data.CONTENT_URI

    private fun getProjection(): Array<String> {
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

    private suspend fun buildWhereAsync(query: String): ContentWhere {
        val where = ContentWhere()
        where.add("${ContactsContract.Data.MIMETYPE} = ?", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        if (query.isNotEmpty()) {
            QueryHelper.parseAsync(query).forEach {
                when (it.name) {
                    "text" -> {
                        where.add("${ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME} LIKE ?", "%${it.value}%")
                    }

                    "ids" -> {
                        where.addIn(ContactsContract.Data.RAW_CONTACT_ID, it.value.split(","))
                    }

                    "id" -> {
                        where.addEqual(ContactsContract.Data.RAW_CONTACT_ID, it.value)
                    }
                }
            }
        }

        return where
    }

    suspend fun countAsync(context: Context, query: String): Int {
        return context.contentResolver.count(uriExternal, buildWhereAsync(query))
    }

    fun deleteByIdsAsync(
        context: Context,
        ids: Set<String>,
    ) {
        ids.chunked(30).forEach { chunk ->
            val selection = "${ContactsContract.Data.RAW_CONTACT_ID} IN (${StringHelper.getQuestionMarks(chunk.size)})"
            val selectionArgs = chunk.map { it }.toTypedArray()
            context.contentResolver.delete(uriExternal, selection, selectionArgs)
        }
    }

    suspend fun getByIdAsync(
        context: Context,
        id: String,
    ): DContact? {
        return searchAsync(context, "id=$id", 1, 0).firstOrNull()
    }

    suspend fun getIdsAsync(context: Context, query: String): Set<String> {
        val where = buildWhereAsync(query)
        return context.contentResolver.getSearchCursor(uriExternal, arrayOf(ContactsContract.Data.RAW_CONTACT_ID), where)?.map { cursor, cache ->
            cursor.getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache)
        }?.toSet() ?: emptySet()
    }

    fun updateAsync(
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

    fun createAsync(contact: ContactInput): String {
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

    suspend fun searchAsync(
        context: Context,
        query: String,
        limit: Int,
        offset: Int,
    ): List<DContact> {
        val contentMap = ContentHelper.getMap(context)
        return context.contentResolver.getPagingCursor(
            uriExternal, getProjection(), buildWhereAsync(query), limit, offset,
            SortBy(ContactsContract.Data.DISPLAY_NAME_PRIMARY, SortDirection.ASC)
        )?.map { cursor, cache ->
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
        } ?: emptyList()
    }
}
