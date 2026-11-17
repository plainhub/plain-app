package com.ismartcoding.plain.events

import com.ismartcoding.plain.models.AuthErrorCode

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.graphql.Query
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class EventRecordTest {
    private fun event(type: String, payload: String): EventRecord = JsonHelper.jsonDecode(
        """{"id":"fixture","type":"$type","data":$payload,"clientId":"706c6169-5847-8f52-9073-afe9de5f9211","clientName":"Operator","clientIp":null,"osName":null,"osVersion":null,"appName":null,"appVersion":null,"createdAt":"2026-01-01T00:00:00Z"}"""
    )

    @Test
    fun decodesEveryTypedPayloadAndKeepsTheActorSeparateFromTheTarget() {
        val session = """{"__typename":"SessionEventData","session":{"clientId":"706c6169-15b2-8ac4-83ea-4e6b0c3a4b3c","clientName":"Target","type":"API_TOKEN"}}"""
        val volume = """{"id":"volume","name":"Volume","devicePath":"/dev/generated1","mountPath":"/mnt/generated","fileSystem":"ext4"}"""
        val disk = """{"id":"disk","name":"Disk","path":"/dev/generated","sizeBytes":64000000000}"""
        val fixtures = listOf(
            "LOGIN" to """{"__typename":"LoginEventData","sessionType":"LOGIN"}""",
            "LOGIN_FAILED" to """{"__typename":"LoginFailedEventData","reason":"INVALID_PASSWORD"}""",
            "LOGOUT" to session,
            "REVOKE_SESSION" to session,
            "UPDATE_DEVICE_NAME" to """{"__typename":"DeviceNameEventData","oldName":"Old","newName":"New"}""",
            "MOUNT" to """{"__typename":"VolumeEventData","volume":$volume}""",
            "MOUNT_FAILED" to """{"__typename":"VolumeFailedEventData","volume":$volume,"failure":{"reason":"MOUNT_FAILED","detail":null}}""",
            "UNMOUNT" to """{"__typename":"VolumeEventData","volume":$volume}""",
            "FORMAT_DISK" to """{"__typename":"DiskFormatEventData","disk":$disk,"fileSystem":"ext4"}""",
            "FORMAT_DISK_FAILED" to """{"__typename":"DiskFormatFailedEventData","disk":$disk,"fileSystem":"ext4","failure":{"reason":"FORMAT_DISK_FAILED","detail":null}}""",
        )
        for ((type, payload) in fixtures) {
            val record = event(type, payload)
            assertEquals(type, record.type.name)
            assertEquals("706c6169-5847-8f52-9073-afe9de5f9211", record.clientId)
            assertEquals(record, JsonHelper.jsonDecode<EventRecord>(JsonHelper.jsonEncode(record)))
            when (val data = record.data) {
                is SessionEventData -> assertEquals("706c6169-15b2-8ac4-83ea-4e6b0c3a4b3c", data.session.clientId)
                is DiskFormatEventData -> assertEquals(64000000000, data.disk.sizeBytes)
                is LoginFailedEventData -> assertEquals(AuthErrorCode.INVALID_PASSWORD, data.reason)
                else -> Unit
            }
        }
        assertEquals(EventType.entries.map { it.name }, fixtures.map { it.first })
        assertFails { event("LOGIN_FAILED", """{"__typename":"LoginFailedEventData","reason":"UNKNOWN"}""") }
        assertFails { event("LOGIN", """{"__typename":"LoginEventData","sessionType":"UNKNOWN"}""") }
        assertFails { event("LOGIN", """{"__typename":"UnknownEventData"}""") }
        assertFails { JsonHelper.jsonDecode<EventType>("\"UNKNOWN\"") }
        assertFails { JsonHelper.jsonDecode<EventType>("\"SET_HOSTNAME\"") }
    }

    @Test
    fun queryUsesTheSameMainSchemaOperationAndSelectsAllUnionMembers() {
        val request = Query.events("text:fixture", 50, 50)
        assertTrue(request.operation.query.contains("events(query:"))
        for (name in listOf("LoginEventData", "LoginFailedEventData", "SessionEventData", "DeviceNameEventData", "VolumeEventData", "VolumeFailedEventData", "DiskFormatEventData", "DiskFormatFailedEventData")) {
            assertTrue(request.operation.query.contains("... on $name{"))
        }
        assertTrue(!request.operation.query.contains(" message "))
        assertEquals("events", request.field)
    }
}
