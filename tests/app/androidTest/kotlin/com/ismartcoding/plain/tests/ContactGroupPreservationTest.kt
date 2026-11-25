package com.ismartcoding.plain.tests

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.contacts.ContactInput
import com.ismartcoding.plain.features.contacts.ContactPhoneInput
import com.ismartcoding.plain.features.telephony.ContactPhoneType
import com.ismartcoding.plain.platform.media.provider.ContactMediaStoreHelper
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ContactGroupPreservationTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
    )

    @Test
    fun changingThePhoneNumberDoesNotTouchExistingGroupMembership() {
        val resolver = appContext.contentResolver
        val group = checkNotNull(resolver.insert(ContactsContract.Groups.CONTENT_URI, ContentValues().apply {
            put(ContactsContract.Groups.TITLE, "fixture-group-${UUID.randomUUID()}")
            put(ContactsContract.Groups.GROUP_VISIBLE, 1)
        }))
        try {
            val raw = checkNotNull(resolver.insert(ContactsContract.RawContacts.CONTENT_URI, ContentValues()))
            try {
                val rawId = ContentUris.parseId(raw)
                val groupId = ContentUris.parseId(group)
                val groupType = ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                val phoneType = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                fun insert(mime: String, values: ContentValues) {
                    values.put(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    values.put(ContactsContract.Data.MIMETYPE, mime)
                    checkNotNull(resolver.insert(ContactsContract.Data.CONTENT_URI, values))
                }
                fun values(mime: String): List<String> =
                    checkNotNull(resolver.query(
                        ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.Data.DATA1),
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawId.toString(), mime), null,
                    )).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

                insert(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, "Fixture")
                })
                insert(groupType, ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                })
                insert(phoneType, ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, "5550101")
                })
                val original = values(groupType)
                assertEquals(listOf(groupId.toString()), original)
                val input = ContactInput(
                    prefix = "", firstName = "Fixture", middleName = "", lastName = "", suffix = "",
                    nickname = "", phoneNumbers = listOf(ContactPhoneInput("5550102", ContactPhoneType.MOBILE, "")),
                    emails = emptyList(), addresses = emptyList(), events = emptyList(), source = "",
                    starred = false, notes = "", organization = null, websites = emptyList(), ims = emptyList(),
                )
                ContactMediaStoreHelper.updateAsync(rawId.toString(), input)
                assertEquals(listOf("5550102"), values(phoneType))
                assertEquals(original, values(groupType))
            } finally {
                resolver.delete(raw, null, null)
            }
        } finally {
            resolver.delete(group, null, null)
        }
    }
}
