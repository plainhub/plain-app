package com.ismartcoding.plain.features.contact

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.ismartcoding.plain.MainApp
import com.ismartcoding.lib.extensions.getStringValue
import com.ismartcoding.lib.extensions.queryCursor

object SourceHelper {
    fun getAll(): List<DContactSource> {
        val context = MainApp.instance
        val sources = mutableListOf<DContactSource>()
        setOf(ContactsContract.Groups.CONTENT_URI, ContactsContract.Settings.CONTENT_URI, ContactsContract.RawContacts.CONTENT_URI).forEach {
            fillSourcesFromUri(context, it, sources)
        }

        return sources
    }

    private fun fillSourcesFromUri(context: Context, uri: Uri, sources: MutableList<DContactSource>) {
        context.queryCursor(
            uri, arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            )
        ) { cursor, cache ->
            val name = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_NAME, cache)
            val type = cursor.getStringValue(ContactsContract.RawContacts.ACCOUNT_TYPE, cache)
            if (!sources.any { it.name == name && it.type == type }) {
                sources.add(DContactSource(name, type))
            }
        }
    }
}