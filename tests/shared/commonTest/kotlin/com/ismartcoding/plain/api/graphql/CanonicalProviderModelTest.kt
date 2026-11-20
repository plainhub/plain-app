package com.ismartcoding.plain.api.graphql

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.telephony.SmsType
import com.ismartcoding.plain.models.Package
import com.ismartcoding.plain.models.Sms
import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalProviderModelTest {
    @Test
    fun packageListUsesTheSchemaModelAsTheSelectableItem() {
        val payload = """{"id":"example.app","name":"Example","type":"USER","version":"1","path":"/apps/example.apk","size":12,"certs":[],"installedAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}"""
        val model = JsonHelper.jsonDecode<Package>(payload)
        val item: IData = model
        assertEquals("example.app", item.id)
        assertEquals("/apps/example.apk", model.path)
    }

    @Test
    fun smsReadsCanonicalFieldsWithTheMailboxEnum() {
        val payload = """{"id":"7","body":"Hello","address":"sender","sentAt":"2026-01-01T00:00:00Z","serviceCenter":"","read":true,"threadId":"2","type":"INBOX","subscriptionId":3,"isMms":false,"attachments":[]}"""
        val model = JsonHelper.jsonDecode<Sms>(payload)
        val item: IData = model
        assertEquals("7", item.id)
        assertEquals(SmsType.INBOX, model.type)
        assertEquals("2026-01-01T00:00:00Z", model.sentAt.toString())
    }
}
