package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.data.DDeviceInfo
import com.ismartcoding.plain.data.DDeviceStatus
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.lib.JsonHelper.jsonEncode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Rust public schema reads device identity and runtime state through
 * `SystemProviderHost`, which hands both DTOs over as JSON. kotlinx
 * serialization resolves a serializer from the static type, so a DTO without
 * `@Serializable` throws at the call instead of at compile time — and the
 * failure only surfaces as `Serializer for class 'DDeviceInfo' is not found`
 * inside a GraphQL error, long after the change that removed the annotation.
 */
class DeviceFactsSerializationTest {
    @Test
    fun deviceInfoEncodesForTheHostBridge() {
        val json = jsonEncode(DDeviceInfo().apply {
            name = "pixel"
            platform = DevicePlatform.ANDROID
            model = "panther"
        })
        assertTrue(json.contains("\"name\":\"pixel\""), json)
        assertTrue(json.contains("\"platform\":\"ANDROID\""), json)
        assertTrue(json.contains("\"model\":\"panther\""), json)
    }

    @Test
    fun deviceStatusEncodesForTheHostBridge() {
        val json = jsonEncode(DDeviceStatus().apply { uptimeSec = 42 })
        assertTrue(json.contains("\"uptimeSec\":42"), json)
    }
}