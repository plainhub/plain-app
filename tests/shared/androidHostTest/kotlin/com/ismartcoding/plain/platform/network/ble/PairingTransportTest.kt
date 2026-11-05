package com.ismartcoding.plain.platform.network.ble

import com.ismartcoding.plain.platform.network.ble.server.BleGattServer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PairingTransportTest {
    private var connection: PairingTransport.Connection? = null

    @AfterTest
    fun close() {
        connection?.close()
    }

    private class Server : BleGattServer {
        val starts = mutableListOf<Pair<String, Long>>()
        var stops = 0
        var failStart = false
        override fun start(serviceUuid: String, revision: Long) {
            starts += serviceUuid to revision
            if (failStart) error("SDK rejected advertising")
        }
        override fun stop() { stops++ }
        override fun sendNotification(mac: String, charUuid: String, value: ByteArray): Boolean = true
        override suspend fun sendNotificationBlocking(mac: String, charUuid: String, value: ByteArray): Boolean = true
    }

    @kotlinx.serialization.Serializable
    private data class Command(val lease: String, val revision: Long, val serviceUuid: String?)

    private fun command(revision: Long, active: Boolean = true, serviceUuid: String = "706c6169-2d74-8e53-9a16-f08b62c591d3"): JsonObject =
        com.ismartcoding.plain.lib.JsonHelper.jsonEncodeToElement(Command(checkNotNull(connection).lease, revision, serviceUuid.takeIf { active })).let { it as JsonObject }

    @Test
    fun sdkReceivesExactRustUuidAndOldRevisionsNeverRestartIt() {
        val sdk = Server()
        val current = PairingTransport.connect { sdk }.also { connection = it }
        val uuid = "706c6169-2d74-8e53-9a16-f08b62c591d3"
        assertTrue(current.handle(command(1, serviceUuid = uuid)).jsonPrimitive.boolean)
        assertEquals(uuid, sdk.starts.single().first)
        assertEquals(1L, sdk.starts.single().second)
        assertFalse(current.handle(command(1)).jsonPrimitive.boolean)
        assertEquals(1, sdk.starts.size)
        assertTrue(current.handle(command(2, serviceUuid = uuid)).jsonPrimitive.boolean)
        assertEquals(1, sdk.starts.size)
        assertTrue(current.handle(command(3, active = false)).jsonPrimitive.boolean)
        assertFalse(PairingTransport.isAdvertising())
        assertEquals(1, sdk.stops)
        assertFalse(current.handle(command(2)).jsonPrimitive.boolean)
        assertEquals(1, sdk.starts.size)
    }

    @Test
    fun oldHostCleanupCannotCloseItsReplacement() {
        val firstSdk = Server()
        val first = PairingTransport.connect { firstSdk }.also { connection = it }
        first.handle(command(1))
        val staleCommand = command(100)
        val secondSdk = Server()
        val second = PairingTransport.connect { secondSdk }.also { connection = it }
        assertEquals(1, firstSdk.stops)
        second.handle(command(2))
        assertFailsWith<IllegalStateException> { second.handle(staleCommand) }
        first.close()
        assertEquals(0, secondSdk.stops)
        assertTrue(PairingTransport.isAdvertising())
        assertFailsWith<IllegalStateException> { first.handle(command(3)) }
        assertEquals(1, secondSdk.starts.size)
        second.close()
        assertEquals(1, secondSdk.stops)
        assertFalse(PairingTransport.isAdvertising())
        assertFailsWith<IllegalStateException> { second.handle(command(4)) }
    }

    @Test
    fun malformedCommandsAndFailedCreationDoNotLeaveAnOwnedServer() {
        val failed = Server().also { it.failStart = true }
        var creations = 0
        val replacement = Server()
        val current = PairingTransport.connect { if (creations++ == 0) failed else replacement }.also { connection = it }
        assertFailsWith<IllegalArgumentException> { current.handle(command(0)) }
        assertEquals(0, creations)
        assertFailsWith<IllegalStateException> { current.handle(command(1)) }
        assertEquals(1, failed.stops)
        assertFalse(PairingTransport.isAdvertising())
        current.handle(command(2))
        assertTrue(PairingTransport.isAdvertising())
        assertEquals(1, replacement.starts.size)
    }
}
