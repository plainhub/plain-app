package com.ismartcoding.plain.features.contact

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.provider.ContactsContract
import com.ismartcoding.plain.MainApp
import com.ismartcoding.lib.extensions.getLongValue
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.queryCursor
import java.util.ArrayList

object GroupHelper {
    fun getAll(): List<DGroup> {
        val context = MainApp.instance
        val groups = ArrayList<DGroup>()
        val uri = ContactsContract.Groups.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Groups._ID,
            ContactsContract.Groups.TITLE,
            ContactsContract.Groups.SYSTEM_ID
        )

        val selection = "${ContactsContract.Groups.AUTO_ADD} = ? AND ${ContactsContract.Groups.FAVORITES} = ?"
        val selectionArgs = arrayOf("0", "0")
        context.queryCursor(uri, projection, selection, selectionArgs) { cursor, cache ->
            val id = cursor.getLongValue(ContactsContract.Groups._ID, cache)
            val title = cursor.getStringValue(ContactsContract.Groups.TITLE, cache)

            val systemId = cursor.getStringValue(ContactsContract.Groups.SYSTEM_ID, cache)
            if (groups.map { it.name }.contains(title) && systemId.isNotEmpty()) {
                return@queryCursor
            }

            groups.add(DGroup(id, title))
        }
        return groups
    }

    fun create(name: String, accountName: String, accountType: String): DGroup {
        val context = MainApp.instance
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newInsert(ContactsContract.Groups.CONTENT_URI).apply {
            withValue(ContactsContract.Groups.TITLE, name)
            withValue(ContactsContract.Groups.GROUP_VISIBLE, 1)
            withValue(ContactsContract.Groups.ACCOUNT_NAME, accountName)
            withValue(ContactsContract.Groups.ACCOUNT_TYPE, accountType)
            operations.add(build())
        }

        val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        val rawId = ContentUris.parseId(results[0].uri!!)
        return DGroup(rawId, name)
    }

    fun update(id: String, name: String) {
        val context = MainApp.instance
        val operations = ArrayList<ContentProviderOperation>()
        ContentProviderOperation.newUpdate(ContactsContract.Groups.CONTENT_URI).apply {
            val selection = "${ContactsContract.Groups._ID} = ?"
            val selectionArgs = arrayOf(id)
            withSelection(selection, selectionArgs)
            withValue(ContactsContract.Groups.TITLE, name)
            operations.add(build())
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }

    fun delete(id: String) {
        val context = MainApp.instance
        val operations = ArrayList<ContentProviderOperation>()
        val uri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id.toLong()).buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .build()

        operations.add(ContentProviderOperation.newDelete(uri).build())
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
    }
}