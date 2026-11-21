package com.ismartcoding.plain.platform.media.provider


import com.ismartcoding.plain.appContext
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.ismartcoding.plain.helpers.ContentWhere
import com.ismartcoding.plain.lib.extensions.count
import com.ismartcoding.plain.lib.extensions.getIntValue
import com.ismartcoding.plain.lib.extensions.getSearchCursor
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.getTimeValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.host.provider.*
import com.ismartcoding.plain.platform.contacts.ContactDataHelper
import com.ismartcoding.plain.features.contacts.ContactInput
import com.ismartcoding.plain.platform.contacts.GroupHelper
import com.ismartcoding.plain.platform.contacts.readContactPage

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

    suspend fun countAsync(context: Context, where: ContentWhere): Int = withIO {
        context.contentResolver.count(uriExternal, where)
    }

    suspend fun getIdsAsync(context: Context, where: ContentWhere): Set<String> = withIO {
        context.contentResolver.getSearchCursor(uriExternal, arrayOf(ContactsContract.Data.RAW_CONTACT_ID), where)?.map { cursor, cache ->
            cursor.getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache)
        }?.toSet() ?: emptySet()
    }

    fun updateAsync(
        id: String,
        contact: ContactInput,
    ) {
        val context = appContext
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
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.add(ContactDataHelper.newInsert(id, type, contact.nickname))
        }

        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.phoneNumbers.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.emails.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.addresses.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.ims.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.events.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.add(ContactDataHelper.newInsert(id, type, contact.notes))
        }

        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            contact.organization?.let {
                operations.add(ContactDataHelper.newOrgInsert(id, it))
            }
        }

        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.websites.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newDelete(id, type))
            operations.addAll(contact.groupIds.map { ContactDataHelper.newInsert(id, type, it.value) })
        }

//            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.contactId.toString())
//            val contentValues = ContentValues(2)
//            contentValues.put(ContactsContract.Contacts.STARRED, contact.starred)
//            contentValues.put(ContactsContract.Contacts.CUSTOM_RINGTONE, contact.ringtone)
//            context.contentResolver.update(uri, contentValues, null, null)

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }

    fun createAsync(contact: ContactInput, accountType: String): String {
        val context = appContext
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).apply {
            withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.source)
            withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
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
            operations.add(ContactDataHelper.newInsert(id, type, contact.nickname))
        }

        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.phoneNumbers.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.emails.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.addresses.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.ims.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.events.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.let { type ->
            operations.add(ContactDataHelper.newInsert(id, type, contact.notes))
        }

        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE.let {
            contact.organization?.let {
                operations.add(ContactDataHelper.newOrgInsert(id, it))
            }
        }

        ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.websites.map { ContactDataHelper.newInsert(id, type, it) })
        }

        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE.let { type ->
            operations.addAll(contact.groupIds.map { ContactDataHelper.newInsert(id, type, it.value) })
        }

        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        if (results.isEmpty()) {
            return ""
        }

        return ContentUris.parseId(results[0].uri!!).toString()
    }

    internal suspend fun searchAsync(
        context: Context,
        where: ContentWhere,
        limit: Int,
        offset: Int,
    ): ContactPageFacts = withIO {
        if (limit <= 0) return@withIO ContactPageFacts()
        readContactPage(readRows = {
            // ContactsContract.Data ignores the MediaStore paging bundle. Use
            // the ordered, filtered cursor and load details only for this page.
            context.contentResolver.query(
                uriExternal,
                getProjection(),
                where.toSelection(),
                where.args.toTypedArray(),
                "${ContactsContract.Data.DISPLAY_NAME_PRIMARY} ASC, ${ContactsContract.Data.RAW_CONTACT_ID} ASC",
            )?.use { cursor ->
                buildList {
                    if (cursor.moveToPosition(offset.coerceAtLeast(0))) {
                        val cache = mutableMapOf<String, Int>()
                        do {
                            add(cursor.toContactFacts(cache))
                        } while (size < limit && cursor.moveToNext())
                    }
                }
            } ?: emptyList()
        }, readDetails = { ids -> ContactDataHelper.rows(context, ids) },
            readGroups = { GroupHelper.getAll(context) })
    }

    private fun Cursor.toContactFacts(cache: MutableMap<String, Int>): ContactFacts {
        val accountName = getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME, cache)
        val rawId = getStringValue(ContactsContract.Data.RAW_CONTACT_ID, cache)
        val prefix = getStringValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX, cache)
        val givenName = getStringValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, cache)
        val middleName = getStringValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, cache)
        val familyName = getStringValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, cache)
        val suffix = getStringValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX, cache)
        val photoUri = getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_URI, cache)
        val starred = getIntValue(ContactsContract.CommonDataKinds.StructuredName.STARRED, cache)
        val contactId = getStringValue(ContactsContract.Data.CONTACT_ID, cache)
        val thumbnailUri = getStringValue(ContactsContract.CommonDataKinds.StructuredName.PHOTO_THUMBNAIL_URI, cache)
        val ringtone = getStringValue(ContactsContract.CommonDataKinds.StructuredName.CUSTOM_RINGTONE, cache)
        val updatedAt = getTimeValue(ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP, cache)

        return ContactFacts(
            id = rawId, prefix = prefix, firstName = givenName, middleName = middleName,
            lastName = familyName, suffix = suffix, photoId = photoUri, source = accountName,
            starred = starred != 0, contactId = contactId, thumbnailId = thumbnailUri,
            ringtone = ringtone, updatedAt = updatedAt.toString(),
        )
    }
}
