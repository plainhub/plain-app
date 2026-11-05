package com.ismartcoding.plain.platform.network.ble

import com.ismartcoding.plain.rust.native.RustHostReports

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.Serializable

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.network.BleAvailability
import com.ismartcoding.plain.platform.network.bleAvailabilityFlow
import com.ismartcoding.plain.platform.network.ble.server.BleGattServer
import com.ismartcoding.plain.platform.network.bleTransport
import com.ismartcoding.plain.platform.system.PlatformLock
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object PairingTransport {
    private val lock = PlatformLock()
    private var current: Connection? = null

    fun connect(createServer: () -> BleGattServer = { bleTransport().createServer() }): Connection = lock.withLock {
        current?.closeLocked()
        Connection(createServer).also { current = it }
    }

    fun isAdvertising(): Boolean = lock.withLock { current?.server != null }

    fun sendNotification(mac: String, charUuid: String, value: String): Boolean = lock.withLock {
        current?.server?.sendNotification(mac, charUuid, value.encodeToByteArray()) ?: false
    }

    class Connection internal constructor(private val createServer: () -> BleGattServer) {
        @OptIn(ExperimentalUuidApi::class)
        internal val lease = Uuid.random().toString()
        internal var server: BleGattServer? = null
        private var closed = false
        private var revision = 0L

        fun handle(params: JsonObject): JsonElement {
            val request = JsonHelper.jsonDecodeFromElement<AdvertisingRequest>(params)
            val nextRevision = request.revision
            val serviceUuid = request.serviceUuid
            require(nextRevision > 0)
            return lock.withLock {
                check(!closed && current === this && request.lease == lease) { "BLE advertising connection replaced" }
                if (nextRevision <= revision) return@withLock JsonPrimitive(false)
                revision = nextRevision
                if (serviceUuid == null) {
                    val running = server
                    server = null
                    running?.stop()
                } else {
                    val running = server
                    if (running == null) {
                        val created = createServer()
                        try { created.start(serviceUuid, nextRevision); server = created }
                        catch (error: Exception) { created.stop(); throw error }
                    }
                }
                JsonPrimitive(true)
            }
        }

        suspend fun observeFacts() {
            bleAvailabilityFlow.collect { available ->
                val facts = AdvertisingFacts(available == BleAvailability.READY, lease)
                if (!lock.withLock { !closed && current === this }) return@collect
                try { RustHostReports.bleAdvertisingFacts(JsonHelper.jsonEncode(facts)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("BLE advertising facts", error) }
            }
        }

        fun close() = lock.withLock {
            closeLocked()
            if (current === this) current = null
        }

        internal fun closeLocked() {
            closed = true
            val running = server
            server = null
            try { running?.stop() }
            catch (error: Exception) { LogCat.e("BLE advertising cleanup", error) }
        }
    }
}

@Serializable
private data class AdvertisingRequest(val serviceUuid: String?, val revision: Long, val lease: String)

@Serializable
private data class AdvertisingFacts(val available: Boolean, val lease: String)
