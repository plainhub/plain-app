package com.ismartcoding.plain.events

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.EventRecord
import com.ismartcoding.plain.models.EventType
import com.ismartcoding.plain.graphql.Query
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class EventRecordTest {
    @Test
    fun decodesCanonicalEventsAndRejectsUnknownStatuses() {
        val record = JsonHelper.jsonDecode<EventRecord>("""{"id":"fixture","type":"LOGIN_FAILED","message":"invalid_password","clientId":null,"createdAt":"2026-01-01T00:00:00Z"}""")
        assertEquals(EventType.LOGIN_FAILED, record.type)
        assertEquals(null, record.clientId)
        assertEquals("\"LOGIN_FAILED\"", JsonHelper.jsonEncode(record.type))
        assertFails { JsonHelper.jsonDecode<EventType>("\"UNKNOWN\"") }
        assertEquals(listOf("LOGIN", "LOGIN_FAILED", "LOGOUT", "REVOKE", "SET_HOSTNAME", "UPDATE_DEVICE_NAME", "MOUNT", "MOUNT_FAILED", "UNMOUNT", "FORMAT_DISK", "FORMAT_DISK_FAILED"), EventType.entries.map { it.name })
    }
    @Test
    fun queryUsesTheSameMainSchemaOperation() {
        val request = Query.events("text:fixture", 50, 50)
        assertTrue(request.operation.query.contains("events(query:"))
        assertEquals("events", request.field)
    }
}
