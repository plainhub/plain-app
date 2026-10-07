package com.ismartcoding.plain.httpserver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LoginChatKeyTest {
    @Test
    fun derivesTheSameChatKeyAsTheDesktop() {
        val token = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
        assertEquals("C6I48EcKz92pATWNkQcVzJgTSaLJNmuJfdX/al/qY6M=", deriveLoginChatKey(token))
        assertNotEquals(token, deriveLoginChatKey(token))
    }

}
