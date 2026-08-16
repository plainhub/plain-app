package com.ismartcoding.plain.tests

import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.WsSessionHandle
import com.ismartcoding.plain.httpserver.onlineClientIds
import com.ismartcoding.plain.httpserver.setOnlineClientIds
import com.ismartcoding.plain.httpserver.websocket.WebSocketHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeWsSession(
    override val id: Long,
    override val clientId: String,
    private val onSend: (ByteArray) -> Unit,
) : WsSessionHandle {
    override suspend fun send(bytes: ByteArray) = onSend(bytes)
    override suspend fun close(code: Int, reason: String) {}
    override suspend fun close() {}
}

class WebSocketHelperTest {

    private val sessions = ArrayList<FakeWsSession>()

    private fun registerSession(clientId: String, onSend: (ByteArray) -> Unit): FakeWsSession {
        val session = FakeWsSession(System.nanoTime(), clientId, onSend)
        sessions.add(session)
        HttpServerManager.wsSessions.add(session)
        return session
    }

    @After
    fun tearDown() {
        HttpServerManager.wsSessions.removeAll { s -> sessions.any { it.id == s.id } }
        setOnlineClientIds(emptySet())
    }

    @Test
    fun `send failure on one session does not throw and removes that session`() {
        val aliveReceived = ArrayList<ByteArray>()
        registerSession("dead") { throw IllegalStateException("connection closed") }
        val alive = registerSession("alive") { aliveReceived.add(it) }

        runBlocking {
            WebSocketHelper.sendEventAsync(WebSocketEvent(EventType.SCREEN_MIRRORING, byteArrayOf(1, 2, 3)))
        }

        assertEquals(1, aliveReceived.size)
        val expectedPrefix = byteArrayOf(
            (EventType.SCREEN_MIRRORING.value shr 24).toByte(),
            (EventType.SCREEN_MIRRORING.value shr 16).toByte(),
            (EventType.SCREEN_MIRRORING.value shr 8).toByte(),
            EventType.SCREEN_MIRRORING.value.toByte(),
        )
        assertArrayEquals(expectedPrefix + byteArrayOf(1, 2, 3), aliveReceived[0])
        assertFalse(HttpServerManager.wsSessions.any { it.id != alive.id && it.clientId == "dead" })
        assertTrue(HttpServerManager.wsSessions.contains(alive))
        assertEquals(setOf("alive"), onlineClientIds.value)
    }

    @Test
    fun `binary event is broadcast to all healthy sessions`() {
        val first = ArrayList<ByteArray>()
        val second = ArrayList<ByteArray>()
        registerSession("first") { first.add(it) }
        registerSession("second") { second.add(it) }

        runBlocking {
            WebSocketHelper.sendEventAsync(WebSocketEvent(EventType.SCREEN_MIRRORING, byteArrayOf(9)))
        }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }
}
