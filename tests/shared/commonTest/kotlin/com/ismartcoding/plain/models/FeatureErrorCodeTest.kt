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
        assertEquals(PairingErrorCode.PASSWORD_REQUIRED, decode("PASSWORD_REQUIRED").error)
        assertEquals(PairingErrorCode.INVALID_PASSWORD, decode("INVALID_PASSWORD").error)
        for (code in listOf("invalid_password", "MOUNT_FAILED", "INVALID_PREF_VALUE", "FUTURE_ERROR")) {
            assertFails { decode(code) }
            assertNull(PairingErrorCode.fromWire(code))
        }
    }

    @Test
    fun auth_decodes_only_its_own_uppercase_errors() {
        for (code in listOf("INVALID_PASSWORD", "PEER_IDENTITY_CHANGED", "REMOTE_ACCESS_DISABLED", "REJECTED", "LOGIN_EXPIRED", "TOO_MANY_LOGIN_ATTEMPTS")) {
            val value = JsonHelper.jsonDecode<AuthErrorCode>("\"$code\"")
            assertEquals(code, value.wire)
            assertEquals(value, AuthErrorCode.fromWire(code))
        }
        for (code in listOf("invalid_password", "MOUNT_FAILED", "PASSWORD_REQUIRED", "FUTURE_ERROR")) {
            assertFails { JsonHelper.jsonDecode<AuthErrorCode>("\"$code\"") }
            assertNull(AuthErrorCode.fromWire(code))
        }
    }
}
