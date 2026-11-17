package com.ismartcoding.plain.models

import com.ismartcoding.plain.lib.JsonHelper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class FeatureErrorCodeTest {
    @Test
    fun pairing_decodes_only_its_own_uppercase_errors() {
        fun decode(code: String) = JsonHelper.jsonDecode<PairingProgressNotice>(
            """{"deviceId":"fixture","deviceName":"Fixture","error":"$code"}"""
        )
        val wireValues = setOf(
            "TIMEOUT", "REJECTED", "PASSWORD_REQUIRED", "INVALID_PASSWORD",
            "PEER_IDENTITY_CHANGED", "TOO_MANY_LOGIN_ATTEMPTS", "INTERNAL_ERROR",
        )
        assertEquals(wireValues, PairingErrorCode.entries.map { it.wire }.toSet())
        for (code in wireValues) {
            assertEquals(code, decode(code).error?.wire)
            assertEquals(code, PairingErrorCode.fromWire(code)?.wire)
        }
        for (code in listOf("CANCELED", "FAILED_CONNECT_WS", "REMOTE_ACCESS_DISABLED", "invalid_password", "MOUNT_FAILED", "INVALID_PREF_VALUE", "FUTURE_ERROR")) {
            assertFails { decode(code) }
            assertNull(PairingErrorCode.fromWire(code))
        }
    }

    @Test
    fun auth_decodes_only_its_own_uppercase_errors() {
        val wireValues = setOf(
            "CANCELED", "INTERNAL_ERROR", "INVALID_LOGIN_REQUEST", "INVALID_PASSWORD",
            "PASSWORD_SETUP_REQUIRED", "PEER_IDENTITY_CHANGED", "REJECTED", "REMOTE_ACCESS_DISABLED",
            "TOO_MANY_LOGIN_ATTEMPTS", "TIMEOUT",
        )
        assertEquals(wireValues, AuthErrorCode.entries.map { it.wire }.toSet())
        for (code in wireValues) {
            val value = JsonHelper.jsonDecode<AuthErrorCode>("\"$code\"")
            assertEquals(code, value.wire)
            assertEquals(value, AuthErrorCode.fromWire(code))
        }
        for (code in listOf(
            "LOGIN_EXPIRED", "TOO_MANY_LOGIN_CONNECTIONS", "EVENT_STORAGE_FAILED", "UNAUTHORIZED",
            "invalid_password", "MOUNT_FAILED", "PASSWORD_REQUIRED", "FUTURE_ERROR",
        )) {
            assertFails { JsonHelper.jsonDecode<AuthErrorCode>("\"$code\"") }
            assertNull(AuthErrorCode.fromWire(code))
        }
    }
}
