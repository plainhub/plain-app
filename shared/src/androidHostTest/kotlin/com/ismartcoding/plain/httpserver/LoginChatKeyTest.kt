package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.enums.DeviceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginChatKeyTest {
    @Test
    fun derivesTheSameChatKeyAsTheDesktop() {
        val token = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
        assertEquals("C6I48EcKz92pATWNkQcVzJgTSaLJNmuJfdX/al/qY6M=", deriveLoginChatKey(token))
        assertNotEquals(token, deriveLoginChatKey(token))
    }

    @Test
    fun signsTheChatPairingResultWithoutChangingLegacyLoginSignatures() {
        val response = AuthResponse("phone", AuthStatus.COMPLETED, "key", timestamp = 123L)
        assertEquals("phone|COMPLETED|key|123", response.toSignatureData())
        assertEquals("phone|COMPLETED|key|123|true", response.copy(chatPaired = true).toSignatureData())
    }

    @Test
    fun chatPairingAlwaysRequiresPhoneConfirmation() {
        val login = AuthRequest("password", "PlainApp", "", "macOS", "", false)
        assertFalse(requiresLoginConfirmation(login, false))
        assertTrue(requiresLoginConfirmation(login, true))
        assertTrue(requiresLoginConfirmation(login.copy(peer = AuthPeer(
            "Mac", 8443, DeviceType.COMPUTER, listOf("192.0.2.2"), "key",
        )), false))
    }
}
