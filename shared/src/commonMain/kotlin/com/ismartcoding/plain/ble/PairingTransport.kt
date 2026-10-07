package com.ismartcoding.plain.ble
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.api.clientHeadersMap

import kotlinx.serialization.json.*
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.discover.DDiscoverReply
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.discover.RustNearbyWire
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.ble.server.BleGattServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PairingTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: BleGattServer? = null
    private var awareObserverJob: Job? = null

    fun startAdvertising() {
        if (server != null) return
        val s = bleTransport().createServer()
        s.start()
        server = s
        startAwareObserver()
    }

    fun stopAdvertising() {
        awareObserverJob?.cancel()
        awareObserverJob = null
        server?.stop()
        server = null
    }

    /** Whether the GATT server (advertising) is currently running. */
    fun isAdvertising(): Boolean = server != null

    fun refreshAdvertising() {
        server?.refreshAdvertising()
    }

    fun sendNotification(mac: String, charUuid: String, value: String): Boolean {
        return server?.sendNotification(mac, charUuid, value.encodeToByteArray()) ?: false
    }

    private fun startAwareObserver() {
        if (awareObserverJob?.isActive == true) return
        awareObserverJob = scope.launch {
            TempData.awareRunning.drop(1).collect {
                refreshAdvertising()
            }
        }
    }

    fun scanAndDiscover(): Flow<DNearbyDevice> = flow {
        val scan = RustNearbyWire.beginScan()
        try {
            bleTransport().createScanner().scan(BleUuids.SERVICE_UUID).collect { device ->
                val step = RustNearbyWire.scanSeen(scan, device.id)
                val reply = when (step.getValue("kind").jsonPrimitive.content) {
                    "read" -> RustNearbyWire.scanReply(scan, device.id, step.getValue("generation").jsonPrimitive.content, readDiscoverReply(device))
                    "emit" -> JsonHelper.jsonDecode<DDiscoverReply>(step.getValue("reply").toString())
                    else -> null
                }
                if (reply != null) {
                    PeerCacher.setAwareSupported(reply.id, reply.awareSupported)
                    PeerCacher.setAwareRunning(reply.id, reply.awareRunning)
                    emit(PairingCore.replyToDevice(reply, device))
                }
            }
        } finally { withContext(NonCancellable) { RustNearbyWire.endScan(scan) } }
    }

    private suspend fun readDiscoverReply(device: BleGattClient): String? {
        val scanner = bleTransport().createScanner()
        return try {
            // GATT operations (connect, CCCD writes, chunked reads) share the
            // Bluetooth radio with scanning; pausing discovery for the GATT
            // session prevents scan traffic from starving them into timeouts.
            scanner.pauseScan()
            val api = BleDeviceApi(device, RustBleWire)
            api.ensureConnected()
            if (!api.isConnected()) return null

            val message = RustBleWire.nearby(PairingCore.formatMessage(NearbyMessageType.DISCOVER, "").encodeToByteArray())
            val result = api.requestAsync(BleServices.nearby, message)
            api.disconnect()
            RustBleWire.nearbyBody(result).decodeToString(throwOnInvalidSequence = true).takeIf { it.isNotEmpty() }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            LogCat.e("[BLE] readDiscoverReply error: ${e.message}")
            null
        } finally {
            scanner.resumeScan()
            scanner.teardownConnection(device)
        }
    }

}
