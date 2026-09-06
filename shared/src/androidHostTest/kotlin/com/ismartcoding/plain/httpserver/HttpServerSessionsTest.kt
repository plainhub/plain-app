package com.ismartcoding.plain.httpserver

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpServerSessionsTest {
    private val sessions = mutableListOf<RecordingWsHandle>()

    @AfterTest
    fun tearDown() {
        HttpServerManager.wsSessions.removeAll { s -> sessions.any { it.id == s.id } }
        sessions.clear()
        setOnlineClientIds(emptySet())
    }

    @Test
    fun closeAllWsSessions_closesEverySessionOnce_clearsRegistryAndOnlineIds() {
        repeat(3) { i ->
            RecordingWsHandle(id = i.toLong(), clientId = "client-$i").also {
                sessions.add(it)
                HttpServerManager.wsSessions.add(it)
            }
        }
        setOnlineClientIds(setOf("client-0", "client-2"))

        kotlinx.coroutines.runBlocking { closeAllWsSessions() }

        assertEquals(3, sessions.size)
        sessions.forEach { handle ->
            assertEquals(1, handle.closeCount, "session ${handle.clientId} must be closed exactly once, was ${handle.closeCount}")
        }
        assertTrue(HttpServerManager.wsSessions.isEmpty(), "session registry must be empty after closeAllWsSessions")
        assertTrue(onlineClientIds.value.isEmpty(), "online client ids must be cleared")
    }

    @Test
    fun closeAllWsSessions_withNoSessions_isNoOp() {
        setOnlineClientIds(emptySet())
        kotlinx.coroutines.runBlocking { closeAllWsSessions() }
        assertTrue(HttpServerManager.wsSessions.isEmpty())
        assertTrue(onlineClientIds.value.isEmpty())
    }

    private class RecordingWsHandle(
        override val id: Long,
        override val clientId: String,
    ) : WsSessionHandle {
        var closeCount = 0

        override suspend fun send(bytes: ByteArray) {}

        override suspend fun close(code: Int, reason: String) {
            closeCount++
        }

        override suspend fun close() {
            closeCount++
        }
    }
}
